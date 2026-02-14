package com.example.userapi.aspect;

import com.example.userapi.entity.User;
import com.example.userapi.service.AuditLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;
    private final ExpressionParser parser = new SpelExpressionParser();

    public AuditLoggingAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditLog(JoinPoint joinPoint, Auditable auditable, Object result) {
        if (!(result instanceof User user)) {
            return;
        }

        String detail = null;
        if (!auditable.detail().isEmpty()) {
            EvaluationContext context = new StandardEvaluationContext();
            ((StandardEvaluationContext) context).setVariable("args", joinPoint.getArgs());
            ((StandardEvaluationContext) context).setVariable("result", result);
            detail = parser.parseExpression(auditable.detail()).getValue(context, String.class);
        }

        auditLogService.log(user, auditable.eventType(), detail);
    }
}
