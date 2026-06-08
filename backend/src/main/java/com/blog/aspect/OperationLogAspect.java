package com.blog.aspect;

import com.blog.annotation.OperationLog;
import com.blog.mapper.OperationLogMapper;
import com.blog.utils.IpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = point.proceed();
        long endTime = System.currentTimeMillis();

        try {
            saveLog(point, operationLog, endTime - startTime);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }

        return result;
    }

    private void saveLog(ProceedingJoinPoint point, OperationLog operationLog, long time) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String method = signature.getDeclaringTypeName() + "." + signature.getName();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        com.blog.entity.OperationLog log = new com.blog.entity.OperationLog();
        log.setOperation(operationLog.value());
        log.setMethod(method);

        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                log.setParams(objectMapper.writeValueAsString(args));
            }
        } catch (Exception e) {
            log.setParams("参数序列化失败");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            log.setUserId((Long) authentication.getPrincipal());
        }

        if (request != null) {
            log.setIp(IpUtil.getIpAddress(request));
        }

        operationLogMapper.insert(log);
        
        // 使用Slf4j的log记录日志，而不是实体对象的方法
        OperationLogAspect.log.info("操作日志: {} - {}ms", operationLog.value(), time);
    }
}
