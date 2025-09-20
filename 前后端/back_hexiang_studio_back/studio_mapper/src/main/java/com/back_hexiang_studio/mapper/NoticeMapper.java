package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.NoticeDto;
import com.back_hexiang_studio.dv.dto.NoticeStatusUpdateDto;
import com.back_hexiang_studio.dv.dto.PageNoticeDto;
import com.back_hexiang_studio.dv.vo.NoticeVo;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NoticeMapper {

    /**
     * 返回公告列表
     * @param pageNoticeDto
     * @return
     */
    Page<NoticeVo> list(PageNoticeDto pageNoticeDto);

    /**
     * 获取近一个月的系统公告（限制3条）
     * @return 近一个月的最新3条系统公告
     */
    @Select("SELECT * FROM notice WHERE status = '1' AND publishTime >= DATE_SUB(NOW(), INTERVAL 1 MONTH) ORDER BY publishTime DESC LIMIT 3")
    List<NoticeVo> getRecentNotices();
    
    /**
     * 获取近一个月的活动类型公告
     * @return 近一个月的活动类型公告
     */
    @Select("SELECT * FROM notice WHERE status = '1' AND type = 1 AND publishTime >= DATE_SUB(NOW(), INTERVAL 1 MONTH) ORDER BY publishTime DESC")
    List<NoticeVo> getRecentActivityNotices();
    
    /**
     * 获取近一个月的所有系统公告
     * @return 近一个月的所有系统公告
     */
    @Select("SELECT * FROM notice WHERE status = '1'  AND publishTime >= DATE_SUB(NOW(), INTERVAL 1 MONTH) ORDER BY publishTime DESC")
    List<NoticeVo> getAllRecentNotices();
    
    /**
     * 根据ID获取公告
     * @param id 公告ID
     * @return 公告信息
     */
    @Select("SELECT * FROM notice WHERE noticeId = #{id}")
    NoticeVo getById(@Param("id") Long id);

    /**
     * 添加公告
     */
    @Insert("insert into notice ( title,content,publisher,publishTime,status,type) values (#{title},#{content},#{publisher},#{publishTime},#{status},#{type})")
    void add(NoticeDto noticeDto);

    /**
     * 删除公告（批量）
     * @param ids
     */
    void delete(@Param("ids") List<Long> ids);

    /**
     * 修改公告
     * @param noticeDto
     */
   @Update("update notice set title=#{title},content=#{content},publisher=#{publisher},publishTime=#{publishTime},status=#{status},type=#{type} where noticeId=#{noticeId}")
    void update(NoticeDto noticeDto);
    
    /**
     * 更新公告状态
     * @param noticeId 公告ID
     * @param status 新状态
     * @param publisher 发布者（当状态为发布时）
     * @param publishTime 发布时间（当状态为发布时）
     */
    @Update("UPDATE notice SET status = #{status}, publisher = #{publisher}, publishTime = #{publishTime} WHERE noticeId = #{noticeId}")
    void updateStatus(@Param("noticeId") Long noticeId, 
                      @Param("status") Integer status, 
                      @Param("publisher") String publisher, 
                      @Param("publishTime") LocalDateTime publishTime);
}
