package tech.onova.flagd_admin_server.infrastructure.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;

@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    @Around("@annotation(log)")
    public Object logAround(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        // Log request
        logger.info("{}.{} - Request: {}", className, methodName, getArgumentsAsString(joinPoint.getArgs()));
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Log response
            logger.info("{}.{} - Response: {} ({}ms)", className, methodName, getValueAsString(result), duration);
            
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Log error
            logger.error("{}.{} - Error: {} ({}ms)", className, methodName, e.getMessage(), duration, e);
            
            throw e;
        }
    }
    
    private String getArgumentsAsString(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        return "[" + String.join(", ", java.util.Arrays.stream(args).map(Object::toString).toArray(String[]::new)) + "]";
    }
    
    private String getValueAsString(Object value) {
        if (value == null) {
            return "null";
        }
        
        return value.toString();
    }
}