package io.github.chenyilei2016.maintain.manager.identity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ConsoleUserDO;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.LocalCredentialDO;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ConsoleUserMapper;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.LocalCredentialMapper;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsoleUserService {
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9._-]{2,63}");
    private static final String DUMMY_PASSWORD_HASH = "$2a$12$QvR9fUAZ.NZLWgbQkcPyPO91QxSbLAlq2FaPJREjnWb3H1ZAE1s4i";

    private final ConsoleUserMapper users;
    private final LocalCredentialMapper credentials;
    private final PasswordEncoder passwordEncoder;
    private final LocalLoginAttemptGuard loginAttemptGuard;

    @Transactional(rollbackFor = Exception.class)
    public LocalLoginUser authenticateLocal(String username, String password, String sourceAddress) {
        String loginKey = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        loginAttemptGuard.beforeAuthentication(loginKey, sourceAddress);
        String normalizedUsername;
        try {
            normalizedUsername = normalizeUsername(username);
        } catch (IllegalArgumentException invalidUsername) {
            passwordEncoder.matches(password == null ? "" : password, DUMMY_PASSWORD_HASH);
            loginAttemptGuard.failed(loginKey);
            throw CommonException.createReminderException("账号或密码错误");
        }
        LocalCredentialDO credential = credentials.selectByUsername(normalizedUsername);
        String passwordHash = credential == null ? DUMMY_PASSWORD_HASH : credential.getPasswordHash();
        boolean passwordMatches = password != null && passwordEncoder.matches(password, passwordHash);
        ConsoleUserDO user = credential == null ? null : users.selectById(credential.getUserId());
        if (!passwordMatches || user == null || !ConsoleUserStatus.valueOf(user.getStatus()).canLogin()) {
            loginAttemptGuard.failed(normalizedUsername);
            throw CommonException.createReminderException("账号或密码错误");
        }
        loginAttemptGuard.succeeded(normalizedUsername);
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginTime(now).setUpdateTime(now);
        users.updateById(user);
        return toLoginUser(user);
    }

    public boolean hasActiveLocalAdministrator() {
        return credentials.countActiveAdministrators() > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void bootstrapLocalAdministrator(String username, String displayName, String password) {
        if (hasActiveLocalAdministrator()) return;
        createLocalCredential(username, displayName, password, Set.of(ConsoleRole.ADMIN));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserView createLocalUser(String username, String displayName, String password, Set<ConsoleRole> roles) {
        return view(createLocalCredential(username, displayName, password, roles));
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetLocalPassword(String userId, String password) {
        requirePassword(password);
        LocalCredentialDO credential = credentials.selectById(userId);
        if (credential == null) throw CommonException.createReminderException("用户没有本地登录凭证");
        credential.setPasswordHash(passwordEncoder.encode(password)).setUpdateTime(LocalDateTime.now());
        credentials.updateById(credential);
    }

    public LocalLoginUser requireActive(String userId) {
        ConsoleUserDO user = users.selectById(userId);
        if (user == null) throw CommonException.createReminderException("登录用户不存在，请重新登录");
        requireActive(user);
        return toLoginUser(user);
    }

    public IPage<ConsoleUserDO> page(int page, int size) {
        return users.selectPage(new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 50))),
                Wrappers.<ConsoleUserDO>lambdaQuery().orderByDesc(ConsoleUserDO::getLastLoginTime)
                        .orderByDesc(ConsoleUserDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String userId, ConsoleUserStatus status, Set<ConsoleRole> roles, LocalLoginUser actor) {
        users.lockUserAdministration();
        ConsoleUserDO currentActor = users.selectById(actor.getId());
        if (currentActor == null || !ConsoleUserStatus.valueOf(currentActor.getStatus()).canLogin()
                || !roles(currentActor.getRoles()).contains(ConsoleRole.ADMIN)) {
            throw CommonException.createReminderException("当前管理员状态已变更，请重新登录");
        }
        ConsoleUserDO user = users.selectById(userId);
        if (user == null) throw CommonException.createReminderException("用户不存在");
        if (userId.equals(actor.getId()) && (!status.canLogin() || !roles.contains(ConsoleRole.ADMIN))) {
            throw CommonException.createReminderException("不能禁用自己或移除自己的管理员角色");
        }
        user.setStatus(status.name()).setRoles(JSON.toJSONString(roles)).setUpdateTime(LocalDateTime.now());
        users.updateById(user);
        if (users.countActiveAdministrators() == 0) {
            throw CommonException.createReminderException("系统必须保留至少一个可登录管理员");
        }
    }

    public UserView view(ConsoleUserDO user) {
        return new UserView(user.getId(), user.getProvider(), user.getEmployeeNo(), user.getDisplayName(),
                roles(user.getRoles()), ConsoleUserStatus.valueOf(user.getStatus()), user.getLastLoginTime(),
                user.getCreateTime());
    }

    private void requireActive(ConsoleUserDO user) {
        if (!ConsoleUserStatus.valueOf(user.getStatus()).canLogin()) {
            throw CommonException.createReminderException("当前用户已停用");
        }
    }

    private LocalLoginUser toLoginUser(ConsoleUserDO user) {
        LocalLoginUser loginUser = new LocalLoginUser();
        loginUser.setId(user.getId());
        loginUser.setEmployeeNo(user.getEmployeeNo());
        loginUser.setEmployeeName(user.getDisplayName());
        loginUser.setRoles(roles(user.getRoles()).stream().map(Enum::name).collect(Collectors.toSet()));
        return loginUser;
    }

    private Set<ConsoleRole> roles(String json) {
        if (json == null || json.isBlank()) return Set.of();
        return JSON.parseArray(json, String.class).stream()
                .map(role -> {
                    try {
                        return ConsoleRole.valueOf(role);
                    } catch (IllegalArgumentException unsupported) {
                        return null;
                    }
                }).filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    public record UserView(String id, String provider, String employeeNo, String displayName, Set<ConsoleRole> roles,
                           ConsoleUserStatus status, LocalDateTime lastLoginTime, LocalDateTime createTime) {
    }

    private ConsoleUserDO createLocalCredential(String username, String displayName, String password,
                                                Set<ConsoleRole> roles) {
        String normalizedUsername = normalizeUsername(username);
        if (displayName == null || displayName.isBlank() || displayName.length() > 64) {
            throw new IllegalArgumentException("显示名称长度必须为 1 到 64 个字符");
        }
        requirePassword(password);
        if (credentials.selectByUsername(normalizedUsername) != null) {
            throw CommonException.createReminderException("用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        ConsoleUserDO user = users.selectByEmployeeNo(normalizedUsername);
        if (user == null) {
            user = new ConsoleUserDO().setId(IdUtil.generateSnowFlakeId())
                    .setEmployeeNo(normalizedUsername).setCreateTime(now);
        }
        user.setProvider(AuthenticationProviderType.LOCAL_PASSWORD.name()).setExternalSubject(normalizedUsername)
                .setDisplayName(displayName.trim()).setRoles(JSON.toJSONString(roles))
                .setStatus(ConsoleUserStatus.ACTIVE.name()).setUpdateTime(now);
        if (users.selectById(user.getId()) == null) users.insert(user);
        else users.updateById(user);
        credentials.insert(new LocalCredentialDO().setUserId(user.getId()).setUsername(normalizedUsername)
                .setPasswordHash(passwordEncoder.encode(password)).setCreateTime(now).setUpdateTime(now));
        return user;
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("用户名需为 3 到 64 位小写字母、数字、点、下划线或连字符");
        }
        return normalized;
    }

    private void requirePassword(String password) {
        int bytes = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || bytes > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("密码至少 12 个字符且 UTF-8 编码不超过 72 字节");
        }
    }
}
