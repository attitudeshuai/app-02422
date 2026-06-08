package com.blog.exception;

import com.blog.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * 统一处理系统中的各类异常，返回友好的错误信息给前端
 * 避免将系统内部错误直接暴露给用户
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     * 这是我们自定义的业务异常，直接返回异常信息给用户
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Object> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(403, "权限不足，无法访问该资源");
    }

    /**
     * 认证失败异常（用户名或密码错误）
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Object> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("认证失败 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 内部认证服务异常
     * 需要检查根因是否为账号禁用
     */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public Result<Object> handleInternalAuthenticationServiceException(InternalAuthenticationServiceException e, HttpServletRequest request) {
        log.warn("认证服务异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        // 检查根因是否为账号禁用
        Throwable cause = e.getCause();
        if (cause instanceof DisabledException) {
            return Result.error(401, "账号已被禁用，请联系管理员");
        }
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 账号被禁用异常
     */
    @ExceptionHandler(DisabledException.class)
    public Result<Object> handleDisabledException(DisabledException e, HttpServletRequest request) {
        log.warn("账号被禁用 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(401, "账号已被禁用，请联系管理员");
    }

    /**
     * 参数校验异常（@Valid注解校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 [{}]: {}", request.getRequestURI(), message);
        return Result.error(400, message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Object> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败 [{}]: {}", request.getRequestURI(), message);
        return Result.error(400, message);
    }

    /**
     * 缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型错误 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "参数类型错误: " + e.getName());
    }

    /**
     * 请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "请求数据格式错误，请检查JSON格式");
    }

    /**
     * 请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Object> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求方法不支持 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(405, "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传超限 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "上传文件大小超过限制，最大支持10MB");
    }

    /**
     * 数据库唯一约束冲突异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Object> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("数据重复 [{}]: {}", request.getRequestURI(), e.getMessage());
        String message = "数据已存在，请勿重复提交";
        if (e.getMessage().contains("username")) {
            message = "用户名已存在";
        } else if (e.getMessage().contains("email")) {
            message = "邮箱已被注册";
        }
        return Result.error(400, message);
    }

    /**
     * 数据完整性约束异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Object> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性约束异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "数据操作失败，请检查数据完整性");
    }

    /**
     * SQL异常
     */
    @ExceptionHandler(SQLException.class)
    public Result<Object> handleSQLException(SQLException e, HttpServletRequest request) {
        log.error("数据库异常 [{}]: ", request.getRequestURI(), e);
        return Result.error(500, "数据库操作失败，请稍后重试");
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Object> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 [{}]: ", request.getRequestURI(), e);
        return Result.error(500, "系统处理异常，请稍后重试");
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "参数错误: " + e.getMessage());
    }

    /**
     * 通用异常处理
     * 捕获所有未被上面具体异常处理器捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}]: ", request.getRequestURI(), e);
        
        // 根据异常类型返回更友好的错误信息
        String message = "系统繁忙，请稍后重试";
        
        // 如果是已知的异常类型，提供更具体的提示
        if (e.getMessage() != null) {
            if (e.getMessage().contains("Connection refused")) {
                message = "服务连接失败，请稍后重试";
            } else if (e.getMessage().contains("Timeout")) {
                message = "请求超时，请稍后重试";
            } else if (e.getMessage().contains("OutOfMemory")) {
                message = "系统资源不足，请稍后重试";
            }
        }
        
        return Result.error(500, message);
    }
}
