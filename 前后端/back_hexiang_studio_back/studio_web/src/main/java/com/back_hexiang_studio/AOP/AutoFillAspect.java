package com.back_hexiang_studio.AOP;


import com.back_hexiang_studio.annotation.AutoFill;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.enumeration.OperationType;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

/**
 *切面类
 */
@Aspect //切面
@Component
@Slf4j
public class AutoFillAspect {

    //execution表示切入点表达式，用于指定切面的位置

    //* com.sky.mapper.*.*(..)表示切面位置在com.sky.mapper包下的所有类的所有方法

    //@annotation(com.sky.annotation.AutoFill)表示切面位置在带有AutoFill注解的方法上
    //设置注入点
    @Pointcut("execution(* com.back_hexiang_studio.service.impl.*.*(..)) && @annotation(com.back_hexiang_studio.annotation.AutoFill)")
    public void autoFillPointcut() {}

    //在注入前
    //@Before注解表示该方法是前置通知，用于在目标方法执行前执行
    //当匹配到autoFillPointCut()匹配表达式时，执行通知
    @Before("autoFillPointcut()")
    public void before(JoinPoint joinpoint) {
        log.info("开始公共字段填充：");
        //1.获取当前被拦截的方法上的数据库操作类型
        MethodSignature signature= (MethodSignature) joinpoint.getSignature();///获取方法签名对象
        AutoFill autoFill=signature.getMethod().getAnnotation(AutoFill.class);//获取注解对象,getAnnotation(Autofill.class)获取注解,getAnnotation(注解类型.class)获取注解
        OperationType  operationType=autoFill.value();//获取注解的属性值

        //2.获取当前被拦截的方法上的参数
        Object[] args=joinpoint.getArgs();//获取参数数组
        if(args==null || args.length==0) {//判断参数是否为null或长度为0
            log.warn("公共字段填充失败：参数为空");
            return;//如果参数为null或长度为0，则不执行
        }
        Object entityOrList=args[0];//获取第一个参数，可能是单个实体对象，也可能是List

        //3.准备数据
        LocalDateTime localTime= LocalDateTime.now();
        Long   currentId= UserContextHolder.getCurrentId();

        if (currentId == null) {
            log.warn("公共字段填充失败：当前用户ID为null");
            return; // 如果当前用户ID为null，则不执行
        }

        // 4. 根据参数类型进行填充
        if (entityOrList instanceof List) {
            log.info("对List进行公共字段填充...");
            for (Object entity : (List<?>) entityOrList) {
                fillFields(entity, operationType, localTime, currentId);
            }
        } else {
            log.info("对Object进行公共字段填充...");
            fillFields(entityOrList, operationType, localTime, currentId);
        }
    }

    private void fillFields(Object entity, OperationType operationType, LocalDateTime localTime, Long currentId) {
        if(operationType==OperationType.INSERT) {
            try{
                log.info("执行INSERT操作的公共字段填充 for entity: {}", entity.getClass().getSimpleName());
                // 统一设置创建和更新信息
                setField(entity, "setCreateTime", LocalDateTime.class, localTime);
                setField(entity, "setCreateUser", Long.class, currentId);
                setField(entity, "setUpdateTime", LocalDateTime.class, localTime);
                setField(entity, "setUpdateUser", Long.class, currentId);
                // 专门为Material设置上传信息
                setField(entity, "setUploadTime", LocalDateTime.class, localTime);
                setField(entity, "setUploaderId", Long.class, currentId);

            }catch(Exception e){
                log.error("INSERT操作的公共字段填充失败", e);
            }

        }else if(operationType==OperationType.UPDATE){
            try{
                log.info("执行UPDATE操作的公共字段填充 for entity: {}", entity.getClass().getSimpleName());
                // 只设置更新信息
                setField(entity, "setUpdateTime", LocalDateTime.class, localTime);
                setField(entity, "setUpdateUser", Long.class, currentId);
            }catch(Exception e){
                log.error("UPDATE操作的公共字段填充失败", e);
            }
        }
    }

    private void setField(Object entity, String methodName, Class<?> paramType, Object value) {
        try {
            Method method = entity.getClass().getDeclaredMethod(methodName, paramType);
            method.invoke(entity, value);
        } catch (NoSuchMethodException e) {
            // 方法不存在，说明该实体没有这个字段，静默处理，继续执行
             log.trace("方法 {} 在实体 {} 中不存在，跳过填充。", methodName, entity.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("填充字段失败: 方法 {}, 实体 {}", methodName, entity.getClass().getSimpleName(), e);
        }
    }
}