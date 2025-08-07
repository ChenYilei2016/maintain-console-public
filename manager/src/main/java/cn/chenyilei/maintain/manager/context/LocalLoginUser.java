package cn.chenyilei.maintain.manager.context;

import lombok.Data;

import java.util.List;

/**
 * @author chenyilei
 * @date 2022/10/19 16:02
 */
@Data
public class LocalLoginUser {
    /**
     * 员工ID
     */
    private String employeeId;
    /**
     * 员工姓名
     */
    private String employeeName;
    /**
     * 员工工号
     */
    private String employeeNo;

    private String tenantId;

    /**
     * 一些权限
     */
    private List<String> permissions;

    public static LocalLoginUser mock() {
        LocalLoginUser localLoginUser = new LocalLoginUser();
        localLoginUser.setEmployeeId("1");
        localLoginUser.setEmployeeName("cyl");
        localLoginUser.setEmployeeNo("12600");
        localLoginUser.setTenantId("1000000003264");
        return localLoginUser;
    }

//    public static LocalLoginUser from(LoginUserDto userDto) {
//        LocalLoginUser localLoginUser = new LocalLoginUser();
//        localLoginUser.setEmployeeId(userDto.getEmployeeId().toString());
//        localLoginUser.setEmployeeName(userDto.getEmployeeName());
//        localLoginUser.setEmployeeNo(userDto.getEmployeeNoStr());
//        localLoginUser.setTenantId(userDto.getTenantId());
//        localLoginUser.setOrganizationList(userDto.getOrganizationList());
//        return localLoginUser;
//    }
}
