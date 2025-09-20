package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.vo.NoticeAttachmentVo;
import com.back_hexiang_studio.entity.NoticeAttachment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 公告附件数据访问接口
 */
@Mapper
public interface NoticeAttachmentMapper {
    
    NoticeAttachmentVo getAttachmentById(Long attachmentId);
    
    List<NoticeAttachmentVo> getByNoticeId(Long noticeId);
    
    void insert(NoticeAttachment attachment);
    
    void deleteByNoticeId(Long noticeId);
    
    void deleteBatch(List<Long> ids);
    
    void deleteById(Long id);
}