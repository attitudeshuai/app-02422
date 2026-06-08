package com.blog.interceptor;

import com.blog.annotation.RateLimit;
import com.blog.exception.BusinessException;
import com.blog.utils.IpUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.default-limit:100}")
    private int defaultLimit;

    private final LoadingCache<String, RateLimiter> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String key) {
                    return RateLimiter.create(defaultLimit);
                }
            });

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || !(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit != null) {
            String ip = IpUtil.getIpAddress(request);
            String key = ip + ":" + request.getRequestURI();
            
            RateLimiter rateLimiter = cache.getUnchecked(key);
            if (rateLimit.limit() > 0) {
                rateLimiter.setRate(rateLimit.limit());
            }

            if (!rateLimiter.tryAcquire()) {
                throw new BusinessException("请求过于频繁，请稍后再试");
            }
        }

        return true;
    }
}
