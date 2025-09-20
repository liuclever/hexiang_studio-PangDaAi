package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.SupportContact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SupportContactMapper {
    
    @Select("SELECT * FROM support_contact WHERE status = '1' ORDER BY id ASC")
    List<SupportContact> getAllActiveContacts();
} 