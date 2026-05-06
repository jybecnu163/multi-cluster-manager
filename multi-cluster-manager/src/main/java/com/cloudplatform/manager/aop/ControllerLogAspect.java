package com.cloudplatform.manager.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class ControllerLogAspect {

    @Autowired
    private ObjectMapper objectMapper;

    // 切点：所有继承 BaseController 的类中的 public 方法
    @Around("execution(public * com.cloudplatform.manager.controller.BaseController+.*(..))")
    public Object logInputOutput(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String params = objectMapper.writeValueAsString(request.getParameterMap());
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 打印输入参数（请求体参数可通过 joinPoint.getArgs() 获取）
        Object[] args = joinPoint.getArgs();
        String bodyArgs = args.length > 0 ? objectMapper.writeValueAsString(args) : "[]";

        log.info("[ACCESS] {} {}.{} | params={} | body={}", method, className, methodName, params, bodyArgs);

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // 执行原方法
        long elapsed = System.currentTimeMillis() - start;

        // 打印输出结果
        String response = objectMapper.writeValueAsString(result);
        log.info("[RESPONSE] {}.{} | time={}ms | response={}", className, methodName, elapsed, response);

        return result;
    }
}