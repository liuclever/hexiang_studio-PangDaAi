package  com.back_hexiang_studio.annotation;


import com.back_hexiang_studio.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)//注解在运行时有效
@Target(ElementType.METHOD)//注解在方法上有效
public @interface AutoFill {
   //指定数据库类型USE表示插入时填充，UPDATE表示更新时填充
   OperationType value();
}