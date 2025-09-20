package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.dv.dto.UserCertificateDto;
import com.back_hexiang_studio.dv.dto.UserHonorDto;
import com.back_hexiang_studio.dv.vo.UserCertificateVo;
import com.back_hexiang_studio.dv.vo.UserHonorVo;
import com.back_hexiang_studio.enumeration.FileType;
import com.back_hexiang_studio.mapper.UserCertificateMapper;
import com.back_hexiang_studio.mapper.UserHonorMapper;
import com.back_hexiang_studio.service.UserAchievementService;
import com.back_hexiang_studio.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
// 修正 #1: 导入正确的异常包路径
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.ParamException;
import java.io.IOException;
import com.back_hexiang_studio.entity.UserHonor; // 导入实体类
import com.back_hexiang_studio.utils.DateTimeUtils; // 导入工具类
import com.back_hexiang_studio.entity.UserCertificate;


/**
 * 用户成就服务实现类
 * <p>
 * 实现了对用户荣誉和证书的增删改查逻辑。
 * 所有写操作 (add, update, delete) 都被包裹在事务中，并实现了与文件系统的联动，
 * 确保数据库记录和物理文件的一致性。
 * </p>
 */
@Service
@Slf4j
public class UserAchievementServiceImpl implements UserAchievementService {

    @Autowired
    private UserHonorMapper userHonorMapper;

    @Autowired
    private UserCertificateMapper userCertificateMapper;

    @Override
    public List<UserHonorVo> getHonorsByUserId(Long userId) {
        if (userId == null) throw new ParamException("用户ID不能为空");
        return userHonorMapper.findByUserId(userId);
    }

    @Override
    public List<UserHonorVo> getUserHonors(Long userId) {
        if (userId == null) throw new ParamException("用户ID不能为空");
        return userHonorMapper.findByUserId(userId);
    }

    @Override
    public UserHonorVo getHonorDetail(Long honorId) {
        if (honorId == null) throw new ParamException("荣誉ID不能为空");
        return userHonorMapper.findById(honorId);
    }

    @Override
    public List<UserCertificateVo> getCertificatesByUserId(Long userId) {
        if (userId == null) throw new ParamException("用户ID不能为空");
        return userCertificateMapper.findByUserId(userId);
    }

    @Override
    public List<UserCertificateVo> getUserCertificates(Long userId) {
        if (userId == null) throw new ParamException("用户ID不能为空");
        return userCertificateMapper.findByUserId(userId);
    }

    @Override
    public UserCertificateVo getCertificateDetail(Long certificateId) {
        if (certificateId == null) throw new ParamException("证书ID不能为空");
        return userCertificateMapper.findById(certificateId);
    }

    @Override
    @Transactional
    public void addHonor(UserHonorDto honorDto, MultipartFile file) {
        if (honorDto == null || honorDto.getUserId() == null) throw new ParamException("必要参数缺失");
        if (file == null || file.isEmpty()) throw new ParamException("必须上传附件");

        String filePath = "";
        try {
            filePath = FileUtils.saveFile(file, FileType.USER_HONOR);

            UserHonor userHonor = new UserHonor();

            userHonor.setUserId(honorDto.getUserId());
            userHonor.setHonorName(honorDto.getHonorName());
            userHonor.setHonorLevel(honorDto.getHonorLevel());
            userHonor.setIssueOrg(honorDto.getIssueOrg());
            userHonor.setCertificateNo(honorDto.getCertificateNo());
            userHonor.setDescription(honorDto.getDescription());
            userHonor.setAttachment(filePath);
            userHonor.setOriginalFileName(file.getOriginalFilename());

            if (honorDto.getIssueDate() != null && !honorDto.getIssueDate().isEmpty()) {
                userHonor.setIssueDate(DateTimeUtils.parseToDate(honorDto.getIssueDate()));
            }

            userHonorMapper.insert(userHonor);
        } catch (Exception e) {
            if (!filePath.isEmpty()) {
                FileUtils.deleteFile(filePath);
            }
            log.error("添加用户荣誉时保存文件失败, 回滚文件", e);
            throw new BusinessException("数据保存失败, 请重试");
        }
    }

    @Override
    @Transactional
    public void updateHonor(UserHonorDto honorDto, MultipartFile file) {
        if (honorDto == null || honorDto.getHonorId() == null) throw new ParamException("荣誉ID不能为空");

        UserHonorVo oldHonor = userHonorMapper.findById(honorDto.getHonorId());
        if (oldHonor == null) throw new BusinessException("更新失败, 荣誉不存在");

        String oldImagePath = oldHonor.getAttachment();
        String newImagePath = oldImagePath;
        boolean newFileUploaded = false;

        if (file != null && !file.isEmpty()) {
            try {
                newImagePath = FileUtils.saveFile(file, FileType.USER_HONOR);
                newFileUploaded = true;
            } catch (IOException e) {
                log.error("更新用户荣誉时保存新文件失败", e);
                throw new BusinessException("新文件上传失败, 请重试");
            }
        }

        try {
            UserHonor userHonor = new UserHonor();

            userHonor.setHonorsId(honorDto.getHonorId());
            userHonor.setHonorName(honorDto.getHonorName());
            userHonor.setHonorLevel(honorDto.getHonorLevel());
            userHonor.setIssueOrg(honorDto.getIssueOrg());
            userHonor.setCertificateNo(honorDto.getCertificateNo());
            userHonor.setDescription(honorDto.getDescription());
            userHonor.setAttachment(newImagePath);

            if (newFileUploaded) {
                userHonor.setOriginalFileName(file.getOriginalFilename());
            }

            if (honorDto.getIssueDate() != null && !honorDto.getIssueDate().isEmpty()) {
                userHonor.setIssueDate(DateTimeUtils.parseToDate(honorDto.getIssueDate()));
            }

            userHonorMapper.update(userHonor);

            if (newFileUploaded && oldImagePath != null && !oldImagePath.isEmpty() && !oldImagePath.contains("default-certificate")) {
                FileUtils.deleteFile(oldImagePath);
            }
        } catch (Exception e) {
            if (newFileUploaded) {
                FileUtils.deleteFile(newImagePath);
            }
            log.error("更新用户荣誉数据失败, 回滚文件", e);
            throw new BusinessException("数据更新失败, 请重试");
        }
    }

    @Override
    @Transactional
    public void deleteHonor(Long honorId) {
        if (honorId == null) throw new ParamException("荣誉ID不能为空");

        // Step 1: 先从数据库获取记录，以得到文件路径
        UserHonorVo honor = userHonorMapper.findById(honorId);
        if (honor == null) {
            log.warn("尝试删除一个不存在的荣誉记录, ID: {}", honorId);
            return;
        }

        // Step 2: 先删除数据库记录
        userHonorMapper.delete(honorId);

        // Step 3: 如果数据库删除成功，再删除物理文件
        if (honor.getAttachment() != null && !honor.getAttachment().isEmpty()) {
            FileUtils.deleteFile(honor.getAttachment());
        }
    }

    @Override
    @Transactional
    public void addCertificate(UserCertificateDto certificateDto, MultipartFile file) {
        if (certificateDto == null || certificateDto.getUserId() == null) throw new ParamException("必要参数缺失");
        if (file == null || file.isEmpty()) throw new ParamException("必须上传附件");

        String filePath = "";
        try {
            filePath = FileUtils.saveFile(file, FileType.USER_CERTIFICATE);

            UserCertificate userCertificate = new UserCertificate();
            userCertificate.setUserId(certificateDto.getUserId());
            userCertificate.setCertificateName(certificateDto.getCertificateName());
            userCertificate.setCertificateLevel(certificateDto.getCertificateLevel());
            userCertificate.setIssueOrg(certificateDto.getIssueOrg());
            userCertificate.setCertificateNo(certificateDto.getCertificateNo());
            userCertificate.setDescription(certificateDto.getDescription());
            userCertificate.setVerificationUrl(certificateDto.getVerificationUrl());
            userCertificate.setImageUrl(filePath);
            userCertificate.setOriginalFileName(file.getOriginalFilename());

            if (certificateDto.getIssueDate() != null && !certificateDto.getIssueDate().isEmpty()) {
                userCertificate.setIssueDate(DateTimeUtils.parseToDate(certificateDto.getIssueDate()));
            }
            if (certificateDto.getExpiryDate() != null && !certificateDto.getExpiryDate().isEmpty()) {
                userCertificate.setExpiryDate(DateTimeUtils.parseToDate(certificateDto.getExpiryDate()));
            }

            userCertificateMapper.insert(userCertificate);
        } catch (Exception e) {
            if (!filePath.isEmpty()) {
                FileUtils.deleteFile(filePath);
            }
            log.error("添加用户证书时保存文件失败, 回滚文件", e);
            throw new BusinessException("数据保存失败, 请重试");
        }
    }

    @Override
    @Transactional
    public void updateCertificate(UserCertificateDto certificateDto, MultipartFile file) {
        if (certificateDto == null || certificateDto.getId() == null) throw new ParamException("证书ID不能为空");

        UserCertificateVo oldCertificate = userCertificateMapper.findById(certificateDto.getId());
        if (oldCertificate == null) throw new BusinessException("更新失败, 证书不存在");

        String oldImagePath = oldCertificate.getAttachment();
        String newImagePath = oldImagePath;
        boolean newFileUploaded = false;

        if (file != null && !file.isEmpty()) {
            try {
                newImagePath = FileUtils.saveFile(file, FileType.USER_CERTIFICATE);
                newFileUploaded = true;
            } catch (IOException e) {
                log.error("更新证书时保存新文件失败", e);
                throw new BusinessException("新文件上传失败, 请重试");
            }
        }

        try {
            UserCertificate userCertificate = new UserCertificate();
            userCertificate.setCertificateId(certificateDto.getId());
            userCertificate.setCertificateName(certificateDto.getCertificateName());
            userCertificate.setCertificateLevel(certificateDto.getCertificateLevel());
            userCertificate.setIssueOrg(certificateDto.getIssueOrg());
            userCertificate.setCertificateNo(certificateDto.getCertificateNo());
            userCertificate.setDescription(certificateDto.getDescription());
            userCertificate.setVerificationUrl(certificateDto.getVerificationUrl());

            if (newFileUploaded) {
                userCertificate.setImageUrl(newImagePath);
                userCertificate.setOriginalFileName(file.getOriginalFilename());
            } else {
                userCertificate.setImageUrl(oldImagePath);
            }

            if (certificateDto.getIssueDate() != null && !certificateDto.getIssueDate().isEmpty()) {
                userCertificate.setIssueDate(DateTimeUtils.parseToDate(certificateDto.getIssueDate()));
            }
            if (certificateDto.getExpiryDate() != null && !certificateDto.getExpiryDate().isEmpty()) {
                userCertificate.setExpiryDate(DateTimeUtils.parseToDate(certificateDto.getExpiryDate()));
            }
            userCertificateMapper.update(userCertificate);

            if (newFileUploaded && oldImagePath != null && !oldImagePath.isEmpty()) {
                FileUtils.deleteFile(oldImagePath);
            }
        } catch (Exception e) {
            if (newFileUploaded) {
                FileUtils.deleteFile(newImagePath);
            }
            log.error("更新证书数据失败, 回滚文件", e);
            throw new BusinessException("数据更新失败, 请重试");
        }
    }

    @Override
    @Transactional
    public void deleteCertificate(Long certificateId) {
        if (certificateId == null) throw new ParamException("证书ID不能为空");

        // Step 1: 先从数据库获取记录，以得到文件路径
        UserCertificateVo certificate = userCertificateMapper.findById(certificateId);
        if (certificate == null) {
            log.warn("尝试删除一个不存在的证书记录, ID: {}", certificateId);
            return;
        }

        // Step 2: 先删除数据库记录
        userCertificateMapper.delete(certificateId);

        // Step 3: 如果数据库删除成功，再删除物理文件
        if (certificate.getAttachment() != null && !certificate.getAttachment().isEmpty()) {
            FileUtils.deleteFile(certificate.getAttachment());
        }
    }

    // 修正 #4: 实现接口方法以避免编译错误
    @Override
    public boolean deleteUserHonor(Long honorsId) {
        this.deleteHonor(honorsId);
        return true;
    }

    @Override
    public boolean addUserCertificate(UserCertificateDto userCertificateDto, MultipartFile file) {
        this.addCertificate(userCertificateDto, file);
        return true;
    }

    @Override
    public boolean updateUserCertificate(UserCertificateDto userCertificateDto, MultipartFile file) {
        this.updateCertificate(userCertificateDto, file);
        return true;
    }
} 