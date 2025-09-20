package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.UserCertificateDto;
import com.back_hexiang_studio.dv.vo.UserCertificateVo;
import com.back_hexiang_studio.entity.UserCertificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCertificateMapper {

    /**
     * 根据用户ID查询用户的证书信息列表
     * @param userId 用户ID
     * @return 证书信息列表
     */
    List<UserCertificateVo> findByUserId(@Param("userId") Long userId);

    /**
     * 根据证书ID查询证书信息
     * @param certificateId 证书ID
     * @return 证书信息
     */
    UserCertificateVo findById(@Param("certificateId") Long certificateId);

    /**
     * 添加用户证书信息
     * @param certificate 证书信息实体
     * @return 影响行数
     */
    int insert(UserCertificate certificate);

    /**
     * 更新用户证书信息
     * @param certificate 证书信息实体
     * @return 影响行数
     */
    int update(UserCertificate certificate);

    /**
     * 删除用户证书信息
     * @param certificateId 证书ID
     * @return 影响行数
     */
    int delete(@Param("certificateId") Long certificateId);

    /**
     * 根据用户ID列表批量删除证书信息
     * @param userIds 用户ID列表
     * @return 影响行数
     */
    int deleteByUserIds(@Param("userIds") List<String> userIds);
} 