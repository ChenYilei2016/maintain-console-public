package io.github.chenyilei2016.maintain.manager.context;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * @author chenyilei
 * @date 2022/10/19 16:02
 */
@Data
public class LocalLoginUser {
    /**
     * 员工姓名
     */
    private String employeeName;
    /**
     * 员工工号
     */
    private String employeeNo;

    private Set<String> roles = new HashSet<>();

    public static LocalLoginUser mock() {
        LocalLoginUser localLoginUser = new LocalLoginUser();
        localLoginUser.setEmployeeName("cyl");
        localLoginUser.setEmployeeNo("1");
        localLoginUser.getRoles().add("ADMIN");
        return localLoginUser;
    }
}
