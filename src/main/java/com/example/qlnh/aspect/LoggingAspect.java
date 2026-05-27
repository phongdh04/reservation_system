package com.example.qlnh.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.example.qlnh.services..*(..))")
    public void serviceLayer() {}

    @Around("serviceLayer()")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        log.debug(">>> {}.{}() - args: {}", className, methodName, Arrays.toString(joinPoint.getArgs()));
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("<<< {}.{}() - result: {} ({}ms)", className, methodName,
                    result != null ? result.getClass().getSimpleName() : "null", duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("!!! {}.{}() - exception: {} ({}ms)", className, methodName, e.getMessage(), duration);
            throw e;
        }
    }
}
