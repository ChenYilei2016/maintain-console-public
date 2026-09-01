package io.github.chenyilei2016.maintain.manager.identity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ConsoleUserDO;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ConsoleUserMapper;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsoleUserService {
    private final ConsoleUserMapper users;

    @Transactional(rollbackFor = Exception.class)
    public LocalLoginUser login(ExternalIdentity identity) {
        ConsoleUserDO user = users.selectByExternalIdentity(identity.provider().name(), identity.subject());
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new ConsoleUserDO().setId(IdUtil.generateSnowFlakeId()).setProvider(identity.provider().name())
                    .setExternalSubject(identity.subject()).setEmployeeNo(identity.employeeNo())
                    .setDisplayName(identity.displayName()).setRoles(JSON.toJSONString(identity.initialRoles()))
                    .setStatus(ConsoleUserStatus.ACTIVE.name()).setCreateTime(now).setUpdateTime(now);
            try {
                users.insert(user);
            } catch (DuplicateKeyException concurrentLogin) {
                user = users.selectByExternalIdentity(identity.provider().name(), identity.subject());
                if (user == null) throw concurrentLogin;
            }
        }
        requireActive(user);
        user.setDisplayName(identity.displayName()).setLastLoginTime(now).setUpdateTime(now);
        users.updateById(user);
        return toLoginUser(user);
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
        ConsoleUserDO user = users.selectById(userId);
        if (user == null) throw CommonException.createReminderException("用户不存在");
        if (userId.equals(actor.getId()) && (!status.canLogin() || !roles.contains(ConsoleRole.ADMIN))) {
            throw CommonException.createReminderException("不能禁用自己或移除自己的管理员角色");
        }
        user.setStatus(status.name()).setRoles(JSON.toJSONString(roles)).setUpdateTime(LocalDateTime.now());
        users.updateById(user);
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
}
