package com.back_hexiang_studio.pangDaAi.event;

import lombok.Data;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

/**
 * 实体变更事件
 * 
 *  事件驱动RAG架构的核心事件类
 * 当业务数据发生变更时，发布此事件以触发向量数据库的实时同步
 * 
 * 支持的操作类型：
 * - CREATE: 新增数据，需要向量化并添加到向量数据库
 * - UPDATE: 更新数据，需要重新向量化并更新向量数据库
 * - DELETE: 删除数据，需要从向量数据库中删除对应向量
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0 - 事件驱动RAG架构
 * @since 2025-09-14
 */
@Getter
@EqualsAndHashCode(callSuper = false)  
public class EntityChangeEvent extends ApplicationEvent {
    
    /**
     * 实体类型（如：NOTICE、COURSE、TASK等）
     */
    private final String entityType;
    
    /**
     * 操作类型（CREATE、UPDATE、DELETE）
     */
    private final String operation;
    
    /**
     * 变更的数据对象
     * - CREATE/UPDATE操作时：包含完整的实体对象
     * - DELETE操作时：可以只包含实体ID
     */
    private final Object data;
    
    /**
     * 业务主键ID（用于向量数据库的关联）
     */
    private final Long businessId;
    
    /**
     * 事件发生时间戳
     */
    private final long eventTimestamp;
    
    /**
     * 构造方法
     * 
     * @param source 事件源（通常是发布事件的Service）
     * @param entityType 实体类型
     * @param operation 操作类型
     * @param data 数据对象
     * @param businessId 业务主键ID
     */
    public EntityChangeEvent(Object source, String entityType, String operation, Object data, Long businessId) {
        super(source);
        this.entityType = entityType;
        this.operation = operation;
        this.data = data;
        this.businessId = businessId;
        this.eventTimestamp = System.currentTimeMillis();
    }
    
    /**
     * 便捷构造方法：根据数据对象自动提取业务ID
     * 
     * @param source 事件源
     * @param entityType 实体类型
     * @param operation 操作类型
     * @param data 数据对象
     */
    public EntityChangeEvent(Object source, String entityType, String operation, Object data) {
        super(source);
        this.entityType = entityType;
        this.operation = operation;
        this.data = data;
        this.businessId = extractBusinessId(data);
        this.eventTimestamp = System.currentTimeMillis();
    }
    
    /**
     * 从数据对象中提取业务ID
     * 使用反射获取常见的ID字段
     */
    private Long extractBusinessId(Object data) {
        if (data == null) {
            return null;
        }
        
        try {
            // 尝试获取常见的ID字段
            String[] idFields = {"noticeId", "courseId", "taskId", "materialId", "id"};
            
            for (String fieldName : idFields) {
                try {
                    java.lang.reflect.Field field = data.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object idValue = field.get(data);
                    if (idValue instanceof Long) {
                        return (Long) idValue;
                    } else if (idValue instanceof Integer) {
                        return ((Integer) idValue).longValue();
                    }
                } catch (NoSuchFieldException ignored) {
                    // 继续尝试下一个字段
                }
            }
            
            // 如果都没找到，返回null
            return null;
            
        } catch (Exception e) {
            // 反射失败，返回null
            return null;
        }
    }
    
    /**
     * 判断是否为创建操作
     */
    public boolean isCreateOperation() {
        return "CREATE".equals(operation);
    }
    
    /**
     * 判断是否为更新操作
     */
    public boolean isUpdateOperation() {
        return "UPDATE".equals(operation);
    }
    
    /**
     * 判断是否为删除操作
     */
    public boolean isDeleteOperation() {
        return "DELETE".equals(operation);
    }
    
    /**
     * 获取事件的简要描述（用于日志）
     */
    public String getEventDescription() {
        return String.format("%s-%s [ID:%s]", entityType, operation, businessId);
    }
    
    @Override
    public String toString() {
        return String.format("EntityChangeEvent{entityType='%s', operation='%s', businessId=%s, eventTimestamp=%d}", 
                           entityType, operation, businessId, eventTimestamp);
    }
} 