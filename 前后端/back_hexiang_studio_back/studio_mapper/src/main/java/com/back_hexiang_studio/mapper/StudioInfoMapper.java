package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.vo.StudioInfoVo;
import com.back_hexiang_studio.entity.StudioInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudioInfoMapper {
    
    /**
     * 获取工作室信息
     * @return 工作室信息
     */
    @Select("SELECT id, name, establish_time AS establishTime, director, " +
            "member_count AS memberCount, project_count AS projectCount, " +
            "awards, phone, email, address, room " +
            "FROM studio_info ORDER BY id DESC LIMIT 1")
    StudioInfoVo getStudioInfo();
    
    /**
     * 更新工作室信息
     * @param studioInfo 工作室信息
     */
    @Update("UPDATE studio_info SET name=#{name}, establish_time=#{establishTime}, director=#{director}, " +
            "member_count=#{memberCount}, project_count=#{projectCount}, awards=#{awards}, " +
            "phone=#{phone}, email=#{email}, address=#{address}, room=#{room}, " +
            "update_time=NOW(), update_user=#{updateUser} WHERE id=#{id}")
    void updateStudioInfo(StudioInfo studioInfo);
} 