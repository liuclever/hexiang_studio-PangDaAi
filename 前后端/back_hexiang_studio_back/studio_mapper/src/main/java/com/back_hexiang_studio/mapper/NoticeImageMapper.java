package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.NoticeImage;
import com.back_hexiang_studio.dv.vo.NoticeImageVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告图片Mapper接口
 */
@Mapper
public interface NoticeImageMapper {
    /**
     * 根据公告ID查询图片列表
     * @param noticeId 公告ID
     * @return 图片列表
     */
    List<NoticeImage> selectByNoticeId(@Param("noticeId") Long noticeId);
    
    /**
     * 插入图片
     * @param noticeImage 图片信息
     */
    void insert(NoticeImage noticeImage);
    
    /**
     * 删除公告下的所有图片
     * @param noticeId 公告ID
     * @return 影响行数
     */
    void deleteByNoticeId(@Param("noticeId") Long noticeId);
    
    /**
     * 删除指定图片
     * @param imageId 图片ID
     * @return 影响行数
     */
    void deleteById(@Param("imageId") Long imageId);

    /**
     * 根据公告ID获取图片列表
     * @param noticeId 公告ID
     * @return 图片列表
     */
    List<NoticeImageVo> getByNoticeId(@Param("noticeId") Long noticeId);

    /**
     * 根据公告ID列表批量删除图片
     * @param noticeIds 公告ID列表
     */
    void deleteByNoticeIds(List<Long> noticeIds);
    
    NoticeImageVo getById(Long id);
    
    void deleteBatch(List<Long> ids);
} 