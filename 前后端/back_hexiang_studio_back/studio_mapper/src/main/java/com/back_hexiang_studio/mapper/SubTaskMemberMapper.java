package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.vo.task.SubTaskMemberVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubTaskMemberMapper {
    /**
     * 添加子任务成员
     * @param memberVo
     */
    void insert(SubTaskMemberVo memberVo);

    /**
     * 根据主任务ID删除所有相关的子任务成员
     * @param taskId
     */
    void deleteByTaskId(Long taskId);
}
