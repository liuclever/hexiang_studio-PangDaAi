package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.dv.vo.UserCertificateVo;
import com.back_hexiang_studio.dv.vo.UserHonorVo;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.UserAchievementService;
import com.back_hexiang_studio.utils.FileUtils;
import com.back_hexiang_studio.utils.PathUtils;
import com.back_hexiang_studio.utils.FileValidationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.back_hexiang_studio.dv.dto.UserCertificateDto;
import com.back_hexiang_studio.dv.dto.UserHonorDto;
import static com.back_hexiang_studio.utils.PathUtils.DANGEROUS_FILE_EXTENSIONS;


/**
 * 用户成就控制器
 */
@RestController
@RequestMapping("/admin/achievement")
@Slf4j
public class UserAchievementController {

    @Autowired
    private UserAchievementService userAchievementService;

    @Autowired
    private FileValidationManager fileValidationManager;


    /**
     * 获取用户证书列表
     * @param userId
     * @return
     */
    @GetMapping("/honors")
    public Result<List<UserHonorVo>> getHonorsByUserId(@RequestParam Long userId) {
        List<UserHonorVo> honors = userAchievementService.getHonorsByUserId(userId);
        return Result.success(honors);
    }

    @PostMapping("/honor/add")
    public Result<?> addHonor(@RequestPart("honor") UserHonorDto honorDto, @RequestPart(value = "file", required = false) MultipartFile file) {
        // 验证文件安全性
        if (file != null && !file.isEmpty()) {
            // 使用统一的验证管理器
            Result<?> validationResult = fileValidationManager.validateHonorCertificateFile(file, "honor");
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
        }

        userAchievementService.addHonor(honorDto, file);
        return Result.success("荣誉添加成功");
    }

    @PostMapping("/honor/update")
    public Result<?> updateHonor(@RequestPart("honor") UserHonorDto honorDto, @RequestPart(value = "file", required = false) MultipartFile file) {
        // 验证文件安全性
        if (file != null && !file.isEmpty()) {
            // 使用统一的验证管理器
            Result<?> validationResult = fileValidationManager.validateHonorCertificateFile(file, "honor");
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
        }
        
        userAchievementService.updateHonor(honorDto, file);
        return Result.success("荣誉更新成功");
    }

    @DeleteMapping("/honor/{honorId}")
    public Result<?> deleteHonor(@PathVariable Long honorId) {
        userAchievementService.deleteHonor(honorId);
        return Result.success("荣誉删除成功");
    }

    // --- Certificates ---

    @GetMapping("/certificates")
    public Result<List<UserCertificateVo>> getCertificatesByUserId(@RequestParam Long userId) {
        List<UserCertificateVo> certificates = userAchievementService.getCertificatesByUserId(userId);
        return Result.success(certificates);
    }

    @PostMapping("/certificate/add")
    public Result<?> addCertificate(@RequestPart("certificate") UserCertificateDto certificateDto, @RequestPart(value = "file", required = false) MultipartFile file) {
        // 验证文件安全性
        if (file != null && !file.isEmpty()) {
            // 使用统一的验证管理器
            Result<?> validationResult = fileValidationManager.validateHonorCertificateFile(file, "certificate");
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
        }
        
        userAchievementService.addCertificate(certificateDto, file);
        return Result.success("证书添加成功");
    }

    @PostMapping("/certificate/update")
    public Result<?> updateCertificate(@RequestPart("certificate") UserCertificateDto certificateDto, @RequestPart(value = "file", required = false) MultipartFile file) {
        // 验证文件安全性
        if (file != null && !file.isEmpty()) {
            // 使用统一的验证管理器
            Result<?> validationResult = fileValidationManager.validateHonorCertificateFile(file, "certificate");
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
        }
        
        userAchievementService.updateCertificate(certificateDto, file);
        return Result.success("证书更新成功");
    }

    @DeleteMapping("/certificate/{certificateId}")
    public Result<?> deleteCertificate(@PathVariable Long certificateId) {
        userAchievementService.deleteCertificate(certificateId);
        return Result.success("证书删除成功");
    }
} 