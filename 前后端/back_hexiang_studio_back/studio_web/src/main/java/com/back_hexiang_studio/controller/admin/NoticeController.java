package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.dv.dto.NoticeDto;
import com.back_hexiang_studio.dv.dto.PageNoticeDto;
import com.back_hexiang_studio.entity.NoticeAttachment;
import com.back_hexiang_studio.entity.NoticeImage;
import com.back_hexiang_studio.enumeration.FileType;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.NoticeService;
import com.back_hexiang_studio.utils.FileUtils;
import com.back_hexiang_studio.utils.PathUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.StringUtils;

import java.io.IOException;

import java.util.ArrayList;

import java.util.List;



import com.back_hexiang_studio.utils.FileValidationManager;

/**
 * 公告管理控制器
 * 查看功能对所有人开放，管理功能需要权限控制
 */
@Slf4j
@RestController
@RequestMapping("/admin/notice")
public class NoticeController {
    @Autowired
    private NoticeService noticeService;
    
    @Autowired
    private ObjectMapper objectMapper; // 注入ObjectMapper

    @Autowired
    private FileValidationManager fileValidationManager;


    /**
     * 返回公告列表（分页）
     * @param pageNoticeDto
     * @return
     */
    @RequestMapping("/list")
    public Result<PageResult> list(PageNoticeDto pageNoticeDto){
        PageResult notices=noticeService.list(pageNoticeDto);
        return Result.success(notices);
    }

    /**
     * 获取近一个月的系统公告（限制3条）
     * @return 近一个月的最新3条系统公告
     */
    @RequestMapping("/recent")
    public Result getRecentNotices() {
        List<?> notices = noticeService.getRecentNotices();
        return Result.success(notices);
    }

    /**
     * 获取近一个月的活动类型公告
     * @return 近一个月的活动类型公告
     */
    @RequestMapping("/recent-activities")
    public Result getRecentActivityNotices() {
        List<?> activities = noticeService.getRecentActivityNotices();
        return Result.success(activities);
    }

    /**
     * 获取近一个月的所有系统公告
     * @return 近一个月的所有系统公告
     */
    @RequestMapping("/all-recent")
    public Result getAllRecentNotices() {
        List<?> notices = noticeService.getAllRecentNotices();
        return Result.success(notices);
    }
    
    /**
     * 获取公告详情（包含图片和附件）
     * @param id 公告ID
     * @return 公告详情
     */
    @RequestMapping("/detail")
    public Result getNoticeDetail(Long id) {
        return Result.success(noticeService.getNoticeDetail(id));
    }

    /**
     * 添加公告
     */
    @RequestMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('NOTICE_MANAGE')")
    public Result add(@RequestPart("noticeDto") String noticeDtoString,
                      @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                      @RequestPart(value = "attachmentFiles", required = false) List<MultipartFile> attachmentFiles) {
        try {
            // 手动反序列化 noticeDto
            NoticeDto noticeDto = objectMapper.readValue(noticeDtoString, NoticeDto.class);

            // 验证图片文件
            List<NoticeImage> images = new ArrayList<>();
            if (imageFiles != null && !imageFiles.isEmpty()) {
                for (MultipartFile file : imageFiles) {
                    // 使用统一的验证管理器
                    Result<?> validationResult = fileValidationManager.validateImageFile(file);
                    if (!validationResult.isSuccess()) {
                        return validationResult;
                    }
                    
                    // 保存文件
                    String filePath = FileUtils.saveFile(file, FileType.NOTICE_IMAGE);
                    
                    NoticeImage image = new NoticeImage();
                    image.setImageName(file.getOriginalFilename());
                    image.setImagePath(filePath);
                    image.setImageSize(file.getSize());
                    image.setImageType(file.getContentType());
                    
                    images.add(image);
                }
            }
            
            // 验证附件文件
            List<NoticeAttachment> attachments = new ArrayList<>();
            if (attachmentFiles != null && !attachmentFiles.isEmpty()) {
                for (MultipartFile file : attachmentFiles) {
                    // 使用统一的验证管理器
                    Result<?> validationResult = fileValidationManager.validateFile(file, "attachment");
                    if (!validationResult.isSuccess()) {
                        return validationResult;
                    }
                    
                    // 保存文件
                    String filePath = FileUtils.saveFile(file, FileType.NOTICE_ATTACHMENT);
                    
                    NoticeAttachment attachment = new NoticeAttachment();
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFilePath(filePath);
                    attachment.setFileSize(file.getSize());
                    attachment.setFileType(file.getContentType());
                    attachment.setDownloadCount(0);
                    
                    attachments.add(attachment);
                }
            }
            
            // 调用服务添加公告
            noticeService.addWithFiles(noticeDto, images, attachments);
            return Result.success();
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传或JSON解析失败：" + e.getMessage());
        }
    }

    /**
     * 删除公告(批量)
     */
    @RequestMapping("/delete")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('NOTICE_MANAGE')")
    public Result delete(@RequestBody List<Long> ids){
        noticeService.delete(ids);
        return Result.success();
    }

    /**
     * 修改公告
     */
    @RequestMapping("/update")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('NOTICE_MANAGE')")
    public Result update(@RequestPart("noticeDto") String noticeDtoString,
                         @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                         @RequestPart(value = "attachmentFiles", required = false) List<MultipartFile> attachmentFiles,
                         @RequestPart(value = "keepImageIds", required = false) String keepImageIdsString,
                         @RequestPart(value = "keepAttachmentIds", required = false) String keepAttachmentIdsString) {
        try {
            // 手动反序列化 noticeDto
            NoticeDto noticeDto = objectMapper.readValue(noticeDtoString, NoticeDto.class);

            // 解析ID列表
            List<Long> keepImageIds = new ArrayList<>();
            if (StringUtils.hasText(keepImageIdsString)) {
                keepImageIds = objectMapper.readValue(keepImageIdsString, new TypeReference<List<Long>>(){});
            }

            List<Long> keepAttachmentIds = new ArrayList<>();
            if (StringUtils.hasText(keepAttachmentIdsString)) {
                keepAttachmentIds = objectMapper.readValue(keepAttachmentIdsString, new TypeReference<List<Long>>(){});
            }

            // 上传新图片
            List<NoticeImage> newImages = new ArrayList<>();
            if (imageFiles != null && !imageFiles.isEmpty()) {
                for (MultipartFile file : imageFiles) {
                    String originalFilename = file.getOriginalFilename();
                    if (originalFilename == null) {
                        return Result.error("文件名不能为空");
                    }
                    
                    // 验证文件安全性
                    Result<?> validationResult = PathUtils.validateImageFile(originalFilename);
                    if (!validationResult.isSuccess()) {
                        return validationResult;
                    }
                    
                    // 使用通用的saveFile方法，并指定文件类型
                    String filePath = FileUtils.saveFile(file, FileType.NOTICE_IMAGE);
                    
                    NoticeImage image = new NoticeImage();
                    image.setImageName(originalFilename);
                    image.setImagePath(filePath);
                    image.setImageSize(file.getSize());
                    image.setImageType(file.getContentType());
                    
                    newImages.add(image);
                }
            }
            
            // 上传新附件
            List<NoticeAttachment> newAttachments = new ArrayList<>();
            if (attachmentFiles != null && !attachmentFiles.isEmpty()) {
                for (MultipartFile file : attachmentFiles) {
                    String originalFilename = file.getOriginalFilename();
                    if (originalFilename == null) {
                        return Result.error("文件名不能为空");
                    }
                    
                    // 验证文件安全性
                    if (!PathUtils.isFilenameExtensionSafe(originalFilename)) {
                        String extension = PathUtils.getFileExtension(originalFilename);
                        log.warn("尝试上传不安全的附件文件类型: {}", extension);
                        return Result.error("不允许上传" + extension + "类型的文件，该类型可能存在安全风险");
                    }
                    
                    // 使用通用的saveFile方法，并指定文件类型
                    String filePath = FileUtils.saveFile(file, FileType.NOTICE_ATTACHMENT);
                    
                    NoticeAttachment attachment = new NoticeAttachment();
                    attachment.setFileName(originalFilename);
                    attachment.setFilePath(filePath);
                    attachment.setFileSize(file.getSize());
                    attachment.setFileType(file.getContentType());
                    attachment.setDownloadCount(0); // 初始下载次数为0
                    
                    newAttachments.add(attachment);
                }
            }
            
            // 调用服务更新公告
            noticeService.updateWithFiles(noticeDto, newImages, newAttachments, keepImageIds, keepAttachmentIds);
            return Result.success();
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传或JSON解析失败：" + e.getMessage());
        }
    }
}
