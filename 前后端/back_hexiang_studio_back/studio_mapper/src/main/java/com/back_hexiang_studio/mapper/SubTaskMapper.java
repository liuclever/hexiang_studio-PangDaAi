package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.dv.vo.task.SubTaskVo;
import com.back_hexiang_studio.entity.SubTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubTaskMapper {
    /**
     * 添加子任务
     * @param subTaskVo
     */
    void insert(SubTaskVo subTaskVo);

    /**
     * 根据主任务ID删除所有子任务
     * @param taskId
     */
    void deleteByTaskId(Long taskId);

    /**
     * 更新子任务状态
     * @param subTaskId 子任务ID
     * @param status 状态
     */
    void updateStatus(@Param("subTaskId") Long subTaskId, @Param("status") Integer status);
    
    /**
     * 根据子任务ID查询子任务
     * @param subTaskId 子任务ID
     * @return 子任务实体
     */
    SubTask findById(@Param("subTaskId") Long subTaskId);
}
