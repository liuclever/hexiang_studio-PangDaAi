package com.back_hexiang_studio.service;


import com.back_hexiang_studio.dv.dto.NoticeDto;
import com.back_hexiang_studio.dv.dto.NoticeStatusUpdateDto;
import com.back_hexiang_studio.dv.dto.PageNoticeDto;
import com.back_hexiang_studio.dv.vo.NoticeDetailVo;
import com.back_hexiang_studio.dv.vo.NoticeVo;
import com.back_hexiang_studio.entity.NoticeAttachment;
import com.back_hexiang_studio.entity.NoticeImage;
import com.back_hexiang_studio.result.PageResult;

import java.util.List;


public interface NoticeService {

    /**
     * 分页查询公告
     * @param pageNoticeDto
     * @return
     */
    PageResult list(PageNoticeDto pageNoticeDto);

    /**
     * 添加公告（包含图片和附件）
     * @param noticeDto 公告基本信息
     * @param images 图片列表
     * @param attachments 附件列表
     */
    void addWithFiles(NoticeDto noticeDto, List<NoticeImage> images, List<NoticeAttachment> attachments);

    /**
     * 删除公告（批量）
     * @param ids
     */
    void delete(List<Long> ids);
    
    /**
     * 修改公告（包含图片和附件）
     * @param noticeDto 公告基本信息
     * @param newImages 新增图片列表
     * @param newAttachments 新增附件列表
     * @param keepImageIds 保留的图片ID列表
     * @param keepAttachmentIds 保留的附件ID列表
     */
    void updateWithFiles(NoticeDto noticeDto, List<NoticeImage> newImages, List<NoticeAttachment> newAttachments, 
                         List<Long> keepImageIds, List<Long> keepAttachmentIds);
    
    /**
     * 获取近一个月的系统公告（限制3条）
     * @return 近一个月的最新3条系统公告
     */
    List<NoticeVo> getRecentNotices();
    
    /**
     * 获取近一个月的活动类型公告
     * @return 近一个月的活动类型公告
     */
    List<NoticeVo> getRecentActivityNotices();
    
    /**
     * 获取近一个月的所有系统公告
     * @return 近一个月的所有系统公告
     */
    List<NoticeVo> getAllRecentNotices();
    
    /**
     * 获取公告详情，包含图片和附件信息
     * @param id 公告ID
     * @return 公告详情
     */
    NoticeDetailVo getNoticeDetail(Long id);
    
    /**
     * 更新公告状态
     * @param noticeStatusUpdateDto 状态更新信息
     */
    void updateStatus(NoticeStatusUpdateDto noticeStatusUpdateDto);
}
