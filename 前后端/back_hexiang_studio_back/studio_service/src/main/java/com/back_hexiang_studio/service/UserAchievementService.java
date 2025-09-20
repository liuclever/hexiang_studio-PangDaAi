package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.UserCertificateDto;
import com.back_hexiang_studio.dv.dto.UserHonorDto;
import com.back_hexiang_studio.dv.vo.UserCertificateVo;
import com.back_hexiang_studio.dv.vo.UserHonorVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户成就服务接口，包括荣誉和证书
 */
public interface UserAchievementService {
    
    /**
     * 获取用户荣誉列表
     * @param userId 用户ID
     * @return 荣誉列表
     */
    List<UserHonorVo> getHonorsByUserId(Long userId);

    List<UserHonorVo> getUserHonors(Long userId);

    /**
     * 获取单个荣誉详情
     * @param honorsId 荣誉ID
     * @return 荣誉详情
     */
    UserHonorVo getHonorDetail(Long honorsId);
    
    /**
     * 获取用户证书列表
     * @param userId 用户ID
     * @return 证书列表
     */
    List<UserCertificateVo> getCertificatesByUserId(Long userId);

    List<UserCertificateVo> getUserCertificates(Long userId);

    /**
     * 获取单个证书详情
     * @param certificateId 证书ID
     * @return 证书详情
     */
    UserCertificateVo getCertificateDetail(Long certificateId);
    
    /**
     * 添加用户荣誉
     * @param userHonorDto 荣誉信息
     * @return 是否成功
     */
    void addHonor(UserHonorDto honorDto, MultipartFile file);
    
    /**
     * 更新用户荣誉
     * @param userHonorDto 荣誉信息
     * @return 是否成功
     */
    void updateHonor(UserHonorDto honorDto, MultipartFile file);
    
    /**
     * 删除用户荣誉
     * @param honorsId 荣誉ID
     * @return 是否成功
     */
    void deleteHonor(Long honorId);
    
    /**
     * 添加用户证书
     * @param userCertificateDto 证书信息
     * @return 是否成功
     */
    void addCertificate(UserCertificateDto certificateDto, MultipartFile file);
    
    /**
     * 更新用户证书
     * @param userCertificateDto 证书信息
     * @return 是否成功
     */
    void updateCertificate(UserCertificateDto certificateDto, MultipartFile file);
    
    /**
     * 删除用户证书
     * @param certificateId 证书ID
     * @return 是否成功
     */
    void deleteCertificate(Long certificateId);

    boolean deleteUserHonor(Long honorsId);

    boolean addUserCertificate(UserCertificateDto userCertificateDto, MultipartFile file);

    boolean updateUserCertificate(UserCertificateDto userCertificateDto, MultipartFile file);
}