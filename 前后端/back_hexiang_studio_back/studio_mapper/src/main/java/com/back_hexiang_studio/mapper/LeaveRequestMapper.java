package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.LeaveRequest;
import com.back_hexiang_studio.dv.dto.LeaveRequestQueryDTO;
import com.back_hexiang_studio.dv.vo.LeaveRequestVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 请假申请Mapper
 */
@Mapper
public interface LeaveRequestMapper {

    /**
     * 分页查询请假申请列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<LeaveRequestVO> pageQuery(LeaveRequestQueryDTO queryDTO);

    /**
     * 根据ID查询请假申请
     * @param requestId 申请ID
     * @return 请假申请实体
     */
    @Select("SELECT * FROM leave_request WHERE request_id = #{requestId}")
    LeaveRequest getById(Long requestId);

    /**
     * 更新请假申请状态及审批信息
     * @param leaveRequest 请假申请实体
     */
    void update(LeaveRequest leaveRequest);

    /**
     * 根据ID获取请假申请详情
     * @param requestId 申请ID
     * @return 包含完整信息的视图对象
     */
    LeaveRequestVO getDetailById(Long requestId);

    /**
     * 获取待审批的请假申请数量
     * @return 待审批数量
     */
    int countPendingRequests();

    /**
     * 获取今日已处理的请假申请数量
     * @return 今日已处理数量
     */
    int countTodayProcessedRequests();

    /**
     * 获取指定创建者的待审批请假申请数量
     * @param creatorId 考勤计划创建者ID
     * @return 待审批数量
     */
    int countPendingRequestsByCreator(@Param("creatorId") Long creatorId);

    /**
     * 获取指定创建者的今日已处理请假申请数量
     * @param creatorId 考勤计划创建者ID
     * @return 今日已处理数量
     */
    int countTodayProcessedRequestsByCreator(@Param("creatorId") Long creatorId);

    /**
     * 查询已审批的请假申请记录
     * @param days 查询天数
     * @return 已审批记录列表
     */
    List<LeaveRequestVO> findProcessedRequests(@Param("days") Integer days);

    /**
     * 查询指定创建者的已审批请假申请记录
     * @param days 查询天数
     * @param creatorId 考勤计划创建者ID
     * @return 已审批记录列表
     */
    List<LeaveRequestVO> findProcessedRequestsByCreator(@Param("days") Integer days, @Param("creatorId") Long creatorId);

    /**
     * 新增请假申请
     * @param leaveRequest 请假申请实体
     */
    void insert(LeaveRequest leaveRequest);

    /**
     * 根据ID删除请假申请
     * @param requestId 申请ID
     * @return 删除的行数
     */
    int deleteById(Long requestId);
} 