

package cn.chenyilei.maintain.manager.exceptions;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.helpers.MessageFormatter;

/**
 *
 */
@Data
@NoArgsConstructor
public class CommonException extends RuntimeException {

    protected String errMsg;

    protected String errorCode = "500";

    protected boolean isReminder = false;

    public CommonException(String errMsg) {
        super(errMsg);
        this.errMsg = errMsg;
    }

    public CommonException(Throwable cause) {
        super(cause);
        this.errMsg = cause.getMessage();
    }

    public CommonException(String msg, Throwable e) {
        super(e);
        this.errMsg = msg;
    }


    public CommonException(String errorCode, String errMsg) {
        super(errMsg);
        this.errMsg = errMsg;
        this.errorCode = errorCode;
    }

    public static CommonException createBizException(String msg) {
        return new CommonException(msg);
    }

    public static CommonException createBizException(String format, Object... args) {

        return new CommonException(strFormat(format, args));
    }

    public static CommonException createBizException(Throwable throwable, String format, Object... args) {
        return new CommonException(strFormat(format, args), throwable);
    }


    public static CommonException createReminderException(String msg) {
        CommonException commonException = new CommonException(msg);
        commonException.setReminder(true);
        return commonException;
    }

    public static CommonException createReminderException(String format, Object... args) {
        CommonException commonException = new CommonException(strFormat(format, args));
        commonException.setReminder(true);
        return commonException;
    }

    public static String strFormat(String format, Object... args) {
        return MessageFormatter.arrayFormat(format, args).getMessage();
    }

//    public static CommonException createAlarmException(String format, Object... args) {
//        return new CommonException(BizLogUtil.strFormat(format, args));
//    }
//
//    public static CommonException createAlarmException(Throwable throwable, String format, Object... args) {
//        return new CommonException(BizLogUtil.strFormat(format, args), throwable);
//    }
}
