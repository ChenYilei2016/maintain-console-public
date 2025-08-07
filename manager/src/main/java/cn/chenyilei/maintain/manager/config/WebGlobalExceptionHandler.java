package cn.chenyilei.maintain.manager.config;

import cn.chenyilei.maintain.manager.exceptions.CommonException;
import cn.chenyilei.maintain.manager.pojo.common.AjaxResult;
import cn.chenyilei.maintain.manager.utils.MyProfileUtils;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @author chenyilei
 * @date 2022/08/19 14:53
 */
@Order(value = -1)
@Slf4j
@ControllerAdvice
@ResponseStatus(HttpStatus.OK)
public class WebGlobalExceptionHandler implements EnvironmentAware {
    private Environment environment;

    public WebGlobalExceptionHandler() {
    }

    private static final String PREFIX_REDIRECT = "redirect:";


    @ExceptionHandler(value = CommonException.class)
    @ResponseBody
    public AjaxResult commonException(CommonException ex, HttpServletRequest request) {
        if (ex.isReminder()) {
            log.debug("commonException 异常", ex);
            return AjaxResult.error(ex.getMessage());
        }
        log.error("commonException 异常" + ex.getMessage(), ex);
        return AjaxResult.error(ex.getMessage());
    }

    @ExceptionHandler(value = Throwable.class)
    @ResponseBody
    public AjaxResult throwableException(Throwable ex, HttpServletRequest request) {
        log.error("未知异常", ex);
        return AjaxResult.error(ex.getMessage());
    }


    @ExceptionHandler(value = IllegalArgumentException.class)
    @ResponseBody
    public AjaxResult illegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("IllegalArgumentException 异常", ex);
        return AjaxResult.error("参数异常:" + ex.getMessage());
    }


    //////////////////////////////   ↑↑  业务异常   //////////////////////////////////////////

    /// ///////////////////////////   ↓↓  框架层异常   //////////////////////////////////////////

    @ExceptionHandler(value = ClientAbortException.class)
    @ResponseBody
    public AjaxResult clientAbortException(ClientAbortException ex, HttpServletRequest request) {
        //无视, 已经不需要返回了
        return null;
    }


    @ExceptionHandler(value = {
            java.sql.SQLException.class,
            DataIntegrityViolationException.class,
            org.springframework.jdbc.UncategorizedSQLException.class, //未分类的sql异常
            java.sql.SQLSyntaxErrorException.class,  //sql语法有问题
            org.springframework.jdbc.BadSqlGrammarException.class, //sql语法有问题
            org.springframework.dao.RecoverableDataAccessException.class,
            org.springframework.dao.DataAccessResourceFailureException.class
    })
    @ResponseBody
    public AjaxResult daoSqlThrowable(Throwable ex, HttpServletRequest request) {
        if (MyProfileUtils.isProd(environment)) {
            log.error("daoSqlThrowable", ex);
            //当为生产环境, 不适合把具体的异常信息展示给用户
            return AjaxResult.error("网络错误,如一直有问题,请联系管理员");
        }
        //sql 异常不透露出具体sql
        log.warn("daoSqlThrowable", ex);
        return AjaxResult.error(ex.getMessage());
    }

    /**
     * Controller上一层相关异常
     */
    @ExceptionHandler({
            NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingPathVariableException.class,
            MissingServletRequestParameterException.class,
            TypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HttpMessageNotWritableException.class,
            HttpMediaTypeNotAcceptableException.class,
            ServletRequestBindingException.class,
            ConversionNotSupportedException.class,
            MissingServletRequestPartException.class,
            AsyncRequestTimeoutException.class
    })
    public void handleServletException(Exception e, HttpServletResponse servletAjaxResult) throws IOException {
        boolean hasCommitted = servletAjaxResult.isCommitted();
        log.warn("[handleServletException], hasCommitted:{}", hasCommitted, e);

        if (hasCommitted) {
            return;
        }
        AjaxResult AjaxResult = null;
        if (MyProfileUtils.isProd(environment)) {
            //当为生产环境, 不适合把具体的异常信息展示给用户, 比如404.
            AjaxResult = AjaxResult.error("网络错误, 请注意链接是否正确, 如稍后重试还有问题请联系管理员");
        } else {
            AjaxResult = AjaxResult.error(e.getMessage());
        }
        servletAjaxResult.getWriter().write(JSON.toJSONString(AjaxResult));
        servletAjaxResult.flushBuffer();
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException 校验参数异常", e);
        return wrapperBindingResult(e.getBindingResult());
    }

    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public AjaxResult handleBindException(BindException e) {
        log.warn("BindException 校验参数异常", e);
        return wrapperBindingResult(e.getBindingResult());
    }


    /**
     * 包装绑定异常结果
     *
     * @param bindingResult 绑定结果
     * @return 异常结果
     */
    private AjaxResult wrapperBindingResult(BindingResult bindingResult) {
        StringBuilder msg = new StringBuilder();
        for (ObjectError error : bindingResult.getAllErrors()) {
            if (error instanceof FieldError) {
                msg.append(((FieldError) error).getField()).append(": ");
            }
            msg.append(error.getDefaultMessage() == null ? "" : error.getDefaultMessage());

        }
        return AjaxResult.error(msg.toString());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
