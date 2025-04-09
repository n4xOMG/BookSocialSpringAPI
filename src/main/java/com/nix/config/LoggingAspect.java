package com.nix.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
	private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

	@Pointcut("execution(* com.nix.controller.*.*(..))")
	public void controllerMethods() {
	}

	@Around("controllerMethods()")
	public Object logAroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication != null ? authentication.getName() : "anonymous";

		String methodName = joinPoint.getSignature().getName();
		String className = joinPoint.getTarget().getClass().getSimpleName();

		logger.info("User={} accessed {}.{}", username, className, methodName);

		Object result = joinPoint.proceed();

		logger.info("User={} completed {}.{}", username, className, methodName);
		return result;
	}
}
