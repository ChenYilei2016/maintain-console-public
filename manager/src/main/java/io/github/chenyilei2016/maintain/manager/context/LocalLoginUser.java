package io.github.chenyilei2016.maintain.manager.context;

import lombok.Data;

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

    public static LocalLoginUser mock() {
        LocalLoginUser localLoginUser = new LocalLoginUser();
        localLoginUser.setEmployeeName("cyl");
        localLoginUser.setEmployeeNo("12600");
        return localLoginUser;
    }
}
