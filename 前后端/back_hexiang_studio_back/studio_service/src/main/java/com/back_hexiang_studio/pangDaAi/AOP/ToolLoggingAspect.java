package com.back_hexiang_studio.pangDaAi.AOP;

import com.back_hexiang_studio.context.UserContextHolder;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AI工具调用日志切面
 * 自动记录工具调用的参数、结果和性能指标
 */
@Aspect
@Component
@Slf4j
public class ToolLoggingAspect {

    // 用于存储当前用户ID的ThreadLocal，解决跨线程传递用户上下文问题
    private static final ThreadLocal<Long> TOOL_USER_CONTEXT = new ThreadLocal<>();

    /**
     * 设置工具执行的用户上下文
     * 此方法由AssistantAgent在工具执行前调用
     */
    public static void setToolUserContext(Long userId) {
        TOOL_USER_CONTEXT.set(userId);
    }

    /**
     * 清理工具执行的用户上下文
     */
    public static void clearToolUserContext() {
        TOOL_USER_CONTEXT.remove();
    }

    /**
     * 获取工具执行的用户上下文
     */
    public static Long getToolUserContext() {
        return TOOL_USER_CONTEXT.get();
    }

    /**
     * 定义切点：所有标注了@Tool注解的方法
     */
    @Pointcut("@annotation(dev.langchain4j.agent.tool.Tool)")
    public void toolMethodPointcut() {
    }

    /**
     * 拦截AI工具调用，记录详细日志
     */
    @Around("toolMethodPointcut()")
    public Object logToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录方法开始执行的时间戳（毫秒）
        long startTime = System.currentTimeMillis();
        // 获取目标对象的类名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        //获取被调用的方法名
        String methodName = joinPoint.getSignature().getName();
        //获取方法调用时传入的所有参数
        Object[] args = joinPoint.getArgs();
        
        // 获取工具描述
        dev.langchain4j.agent.tool.Tool toolAnnotation =
                //获取注解对象
                //强制类型转换为MethodSignature
                // Signature是父接口，没有getMethod()方法
               // MethodSignature是子接口，有getMethod()方法
            ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                .getMethod().getAnnotation(dev.langchain4j.agent.tool.Tool.class);
        //toolAnnotation.value()，获取注解的value属性
        String toolDescription = toolAnnotation != null ? java.util.Arrays.toString(toolAnnotation.value()) : "未知工具";
        
        log.info(" AI工具调用开始 - 工具: [[{}]] | 类: {} | 方法: {} | 参数: {}",
                toolDescription, className, methodName, java.util.Arrays.toString(args));

        try {
            // 在工具执行前设置用户上下文
            Long toolUserId = TOOL_USER_CONTEXT.get();
            if (toolUserId != null && UserContextHolder.getCurrentId() == null) {
                UserContextHolder.setCurrentId(toolUserId);
                log.debug(" 在工具执行线程中设置用户上下文: {}", toolUserId);
            }
            
            // 执行实际的工具方法
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;

            // 限制结果输出长度
            String resultStr = result != null ? result.toString() : "null";
            if (resultStr.length() > 200) {
                resultStr = resultStr.substring(0, 200) + "...";
            }
            
            log.info(" AI工具调用成功 - 工具: [[{}]] | 类: {} | 方法: {} | 耗时: {}ms | 结果: {}",
                    toolDescription, className, methodName, duration, resultStr);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(" AI工具调用失败 - 工具: [[{}]] | 类: {} | 方法: {} | 耗时: {}ms | 错误: {}",
                     toolDescription, className, methodName, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 格式化参数用于日志显示
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                String str = (String) arg;
                // 限制字符串长度避免日志过长
                if (str.length() > 100) {
                    sb.append("\"").append(str.substring(0, 100)).append("...\"");
                } else {
                    sb.append("\"").append(str).append("\"");
                }
            } else {
                sb.append(arg.toString());
            }
        }
        
        return sb.toString();
    }

    /**
     * 获取结果长度用于日志统计
     */
    private int getResultLength(Object result) {
        if (result == null) {
            return 0;
        }
        return result.toString().length();
    }

    /**
     * 格式化结果用于DEBUG日志显示
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        String resultStr = result.toString();
        // 限制结果长度避免日志过长
        if (resultStr.length() > 500) {
            return resultStr.substring(0, 500) + "...";
        }
        
        return resultStr;
    }
} 