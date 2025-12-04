package app.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


 //AOP Aspect for centralized logging.
 // Automatically logs method execution for all service and controller methods.

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    
    @Pointcut("execution(* app.service.*.*(..))")
    public void serviceMethods() {}

    
    @Pointcut("execution(* app.controller.*.*(..))")
    public void controllerMethods() {}

    
    @Around("serviceMethods() || controllerMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get method information
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
    
        log.debug("→ Entering method: {}.{}()", className, methodName);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Execute the actual method
            Object result = joinPoint.proceed();
            
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.debug("← Exiting method: {}.{}() - Execution time: {} ms", 
                className, methodName, executionTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("✗ Exception in method: {}.{}() - Error: {}", 
                className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}


