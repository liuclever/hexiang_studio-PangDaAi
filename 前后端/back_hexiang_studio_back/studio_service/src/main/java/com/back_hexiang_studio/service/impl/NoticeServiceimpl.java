package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.dv.dto.NoticeDto;
import com.back_hexiang_studio.dv.dto.NoticeStatusUpdateDto;
import com.back_hexiang_studio.dv.dto.PageNoticeDto;
import com.back_hexiang_studio.dv.vo.NoticeAttachmentVo;
import com.back_hexiang_studio.dv.vo.NoticeDetailVo;
import com.back_hexiang_studio.dv.vo.NoticeImageVo;
import com.back_hexiang_studio.dv.vo.NoticeVo;
import com.back_hexiang_studio.entity.NoticeAttachment;
import com.back_hexiang_studio.entity.NoticeImage;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.mapper.NoticeAttachmentMapper;
import com.back_hexiang_studio.mapper.NoticeImageMapper;
import com.back_hexiang_studio.mapper.NoticeMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.service.NoticeService;
import com.back_hexiang_studio.utils.DateTimeUtils;
import com.back_hexiang_studio.utils.FileUtils;
import com.back_hexiang_studio.utils.NotificationUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 公告(Notice)服务实现类
 *
 * @author Gemini
 * @since 2024-07-12
 */
@Service
@Slf4j
public class NoticeServiceimpl implements NoticeService {
    @Autowired
    private NoticeMapper noticeMapper;
    
    @Autowired
    private NoticeImageMapper noticeImageMapper;
    
    @Autowired
    private NoticeAttachmentMapper noticeAttachmentMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询公告
     * @param pageNoticeDto
     * @return
     */
    @Override
    public PageResult list(PageNoticeDto pageNoticeDto) {
        // 构建缓存key
        StringBuilder cacheKeyBuilder = new StringBuilder("notice:list:");

        // 使用安全的分页参数构建key, 并处理null情况
        Integer page = pageNoticeDto.getPage() == null ? 1 : pageNoticeDto.getPage();
        Integer pageSize = pageNoticeDto.getPageSize() == null ? 10 : pageNoticeDto.getPageSize();
        cacheKeyBuilder.append(page).append(":").append(pageSize);
                
        if (StringUtils.hasText(pageNoticeDto.getTitle())) {
            cacheKeyBuilder.append(":title:").append(pageNoticeDto.getTitle());
        }
        if (pageNoticeDto.getType() != null && StringUtils.hasText(pageNoticeDto.getType())) {
            cacheKeyBuilder.append(":type:").append(pageNoticeDto.getType());
        }
        if (StringUtils.hasText(pageNoticeDto.getStatus())) {
            cacheKeyBuilder.append(":status:").append(pageNoticeDto.getStatus());
        }
        if (StringUtils.hasText(pageNoticeDto.getBeginTime())) {
            cacheKeyBuilder.append(":begin:").append(pageNoticeDto.getBeginTime());
        }
        if (StringUtils.hasText(pageNoticeDto.getEndTime())) {
            cacheKeyBuilder.append(":end:").append(pageNoticeDto.getEndTime());
        }
        
        String cacheKey = cacheKeyBuilder.toString();
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            // 🔧 优化：频繁的缓存命中，降级为DEBUG
            log.debug("从缓存获取公告列表");
            return (PageResult) cacheResult;
        }
        
        // 🔧 优化：缓存未命中降级为DEBUG，减少日志噪音
        log.debug("缓存未命中，从数据库查询公告列表");
    
        PageHelper.startPage(page, pageSize);
        Page<NoticeVo> noticeList = noticeMapper.list(pageNoticeDto);

        // 为每个公告加载图片和附件
        noticeList.forEach(notice -> {
            notice.setImages(noticeImageMapper.getByNoticeId(notice.getNoticeId()));
            notice.setAttachments(noticeAttachmentMapper.getByNoticeId(notice.getNoticeId()));
        });
        
        PageInfo<NoticeVo> pages = new PageInfo<>(noticeList);

        List<NoticeVo> list = noticeList.getResult(); // 使用 getResult() 获取列表
        PageResult result = new PageResult(pages.getTotal(), list);
        
        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        
        return result;
    }

    /**
     * 删除公告（批量），并删除关联的文件
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }


        List<String> filePathsToDelete = new ArrayList<>();

        for (Long noticeId : ids) {
            // a. 查询关联的图片，并将它们的 `filePath` 添加到 `filePathsToDelete` 列表中。

            List<NoticeImageVo> images = noticeImageMapper.getByNoticeId(noticeId);
            if (images != null) {
                images.forEach(img -> filePathsToDelete.add(img.getFilePath()));
            }

            // b. 查询关联的附件，并将它们的 `filePath` 添加到 `filePathsToDelete` 列表中。
            //    - 调用 `noticeAttachmentMapper.getByNoticeId(noticeId)`
            List<NoticeAttachmentVo> attachments = noticeAttachmentMapper.getByNoticeId(noticeId);
            if (attachments != null) {
                attachments.forEach(att -> filePathsToDelete.add(att.getFilePath()));
            }


            // a. 删除这个公告ID关联的所有图片记录
            //    - 调用 `noticeImageMapper.deleteByNoticeId(noticeId)`
            noticeImageMapper.deleteByNoticeId(noticeId);

            // b. 删除这个公告ID关联的所有附件记录
            //    - 调用 `noticeAttachmentMapper.deleteByNoticeId(noticeId)`
            noticeAttachmentMapper.deleteByNoticeId(noticeId);
        }


        // 调用 `noticeMapper.delete(ids)` 来一次性删除所有指定的公告。
        noticeMapper.delete(ids);


        // 遍历 `filePathsToDelete` 列表，对每个文件路径调用 `FileUtils.deleteFile(filePath)`。
        for (String filePath : filePathsToDelete) {
            if (filePath != null && !filePath.isEmpty()) {
                FileUtils.deleteFile(filePath);
            }
        }


        // 删除操作清除所有相关的公告缓存。
        clearNoticeCache();
    }
    
    /**
     * 获取近一个月的系统公告（限制3条）
     * @return 近一个月的最新3条系统公告
     */
    @Override
    public List<NoticeVo> getRecentNotices() {
        // 定义缓存key
        String cacheKey = "notice:recent";
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取近期公告");
            return (List<NoticeVo>) cacheResult;
        }
        
        log.info("缓存未命中，从数据库查询近期公告");
        
        List<NoticeVo> notices = noticeMapper.getRecentNotices();
        
        // 将结果存入缓存，设置30分钟过期
        redisTemplate.opsForValue().set(cacheKey, notices, 30, TimeUnit.MINUTES);
        
        return notices;
    }
    
    /**
     * 获取近一个月的活动类型公告
     * @return 近一个月的活动类型公告
     */
    @Override
    public List<NoticeVo> getRecentActivityNotices() {
        // 定义缓存key
        String cacheKey = "notice:recentActivity";
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取近期活动公告");
            return (List<NoticeVo>) cacheResult;
        }
        
        log.info("缓存未命中，从数据库查询近期活动公告");
        
        List<NoticeVo> notices = noticeMapper.getRecentActivityNotices();
        
        // 将结果存入缓存，设置30分钟过期
        redisTemplate.opsForValue().set(cacheKey, notices, 30, TimeUnit.MINUTES);
        
        return notices;
    }
    
    /**
     * 获取近一个月的所有系统公告
     * @return 近一个月的所有系统公告
     */
    @Override
    public List<NoticeVo> getAllRecentNotices() {
        // 定义缓存key
        String cacheKey = "notice:allRecent";
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取所有近期公告");
            return (List<NoticeVo>) cacheResult;
        }
        
        log.info("缓存未命中，从数据库查询所有近期公告");
        
        List<NoticeVo> notices = noticeMapper.getAllRecentNotices();
        
        // 将结果存入缓存，设置30分钟过期
        redisTemplate.opsForValue().set(cacheKey, notices, 30, TimeUnit.MINUTES);
        
        return notices;
    }
    
    /**
     * 获取公告详情，包含关联的图片和附件信息。
     * <p>
     * 此方法会首先尝试从Redis缓存中获取数据。如果缓存未命中，则从数据库查询。
     * <b>重要：</b> 此方法返回的图片和附件对象中，URL相关的字段 (`filePath`, `url`等)
     * 将包含由 `FileUtils` 生成的、未加工的<b>相对路径</b>，例如："notice/image/2024/07/12/file.jpg"。
     * URL的拼接工作已移至前端处理。
     * </p>
     *
     * @param id 要查询的公告ID。
     * @return 包含完整详情的 {@link NoticeDetailVo} 对象；如果公告不存在，则返回 null。
     */
    @Override
    public NoticeDetailVo getNoticeDetail(Long id) {
        // 定义缓存key
        String cacheKey = "notice:detail:" + id;
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            //  频繁的缓存命中，降级为DEBUG
            log.debug("从缓存获取公告详情");
            return (NoticeDetailVo) cacheResult;
        }
        
        // 缓存未命中降级为DEBUG，减少日志噪音
        log.debug("缓存未命中，从数据库查询公告详情");
        
        // 获取公告基本信息
        NoticeVo noticeVo = noticeMapper.getById(id);
        if (noticeVo == null) {
            return null;
        }
        
        // 构建详情对象
        NoticeDetailVo detailVo = new NoticeDetailVo();
        BeanUtils.copyProperties(noticeVo, detailVo);
        
        // 由于NoticeVo中的publishTime已经是格式化好的字符串，直接赋值即可
        detailVo.setPublishTime(noticeVo.getPublishTime());
        
        // 获取图片信息
        List<NoticeImageVo> images = noticeImageMapper.getByNoticeId(id);
        // 注意：这里不再拼接URL，直接使用数据库中的相对路径
        detailVo.setImages(images);
        
        // 获取附件信息
        List<NoticeAttachmentVo> attachments = noticeAttachmentMapper.getByNoticeId(id);
        // 注意：这里不再拼接URL，直接使用数据库中的相对路径
        detailVo.setAttachments(attachments);
        
        // 将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(cacheKey, detailVo, 5, TimeUnit.MINUTES);
        
        return detailVo;
    }

    /**
     * 添加公告（包含图片和附件）
     * @param noticeDto 公告基本信息
     * @param images 图片列表
     * @param attachments 附件列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWithFiles(NoticeDto noticeDto, List<NoticeImage> images, List<NoticeAttachment> attachments) {
        String publisherName = null; // 初始化为 null
        Long userId = null; // 用户ID
        
        // 只有在状态为"已发布"时，才设置发布时间和发布者
        if(noticeDto.getStatus()==1){
            noticeDto.setPublishTime(LocalDateTime.now());
            
            // 1. 从 UserContextHolder 获取用户 ID
            userId = UserContextHolder.getCurrentId();
            
            // 2. 根据 ID 查询用户以获取姓名
            if (userId != null) {
                User user = userMapper.getUserById(userId);
                if (user != null && StringUtils.hasText(user.getName())) {
                    publisherName = user.getName();
                }
            }
            // 如果获取不到，则使用默认值或保持为null，取决于业务逻辑，这里我们设置为 "系统"
            if (publisherName == null) {
                 publisherName = "系统";
            }
            noticeDto.setPublisher(publisherName);
        }
        
        noticeMapper.add(noticeDto);
        
        Long noticeId = noticeDto.getNoticeId();
        
        if (images != null && !images.isEmpty()) {
            for (NoticeImage image : images) {
                image.setNoticeId(noticeId);
                noticeImageMapper.insert(image);
            }
        }
        
        if (attachments != null && !attachments.isEmpty()) {
            for (NoticeAttachment attachment : attachments) {
                attachment.setNoticeId(noticeId);
                noticeAttachmentMapper.insert(attachment);
            }
        }
        
        // 如果是已发布状态，创建通知
        if (noticeDto.getStatus() == 1) {
            try {
                // 确定通知重要性
                Integer importance = noticeDto.getType() == 0 ? 1 : 0; // 通知类型为0时(系统通知)设为重要
                
                // 创建通知
                NotificationUtils.createAnnouncementNotification(
                    noticeDto.getTitle(),
                    noticeDto.getContent(),
                    noticeId,
                    userId,
                    importance
                );
                log.info("成功为公告ID: {} 创建系统通知", noticeId);
            } catch (Exception e) {
                log.error("为公告创建通知失败: {}", e.getMessage(), e);
                // 通知创建失败不影响公告发布
            }
        }
        
        clearNoticeCache();
    }

    /**
     * 修改公告（包含图片和附件）
     * @param noticeDto 公告基本信息
     * @param newImages 新增图片列表
     * @param newAttachments 新增附件列表
     * @param keepImageIds 保留的图片ID列表
     * @param keepAttachmentIds 保留的附件ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithFiles(NoticeDto noticeDto, List<NoticeImage> newImages, List<NoticeAttachment> newAttachments, 
                              List<Long> keepImageIds, List<Long> keepAttachmentIds) {
        String publisherName = null; // 初始化为 null
        Long userId = null; // 用户ID

        // 只有在状态为"已发布"时，才设置发布时间和发布者
        if(noticeDto.getStatus()==1){
            noticeDto.setPublishTime(LocalDateTime.now());

            // 1. 从 UserContextHolder 获取用户 ID
            userId = UserContextHolder.getCurrentId();

            // 2. 根据 ID 查询用户以获取姓名
            if (userId != null) {
                User user = userMapper.getUserById(userId);
                if (user != null && StringUtils.hasText(user.getName())) {
                    publisherName = user.getName();
                }
            }
            // 如果获取不到，则设置为 "系统"
            if (publisherName == null) {
                publisherName = "系统";
            }
            noticeDto.setPublisher(publisherName);
        }

        Long noticeId = noticeDto.getNoticeId();
        if (noticeId == null) {
            return;
        }

        // 获取原公告状态
        NoticeVo oldNotice = noticeMapper.getById(noticeId);
        boolean wasPublished = oldNotice != null && oldNotice.getStatus() == 1;
        boolean isPublishedNow = noticeDto.getStatus() == 1;

        // 1. 清理不再需要的旧图片
        List<NoticeImageVo> oldImages = noticeImageMapper.getByNoticeId(noticeId);
        if (oldImages != null && !oldImages.isEmpty()) {
            List<Long> imagesToDeleteIds = oldImages.stream()
                    .map(NoticeImageVo::getImageId)
                    .filter(id -> keepImageIds == null || !keepImageIds.contains(id))
                    .collect(Collectors.toList());

            if (!imagesToDeleteIds.isEmpty()) {
                List<String> imageFilesToDelete = imagesToDeleteIds.stream()
                        .map(id -> noticeImageMapper.getById(id).getFilePath())
                        .collect(Collectors.toList());
                noticeImageMapper.deleteBatch(imagesToDeleteIds);
                imageFilesToDelete.forEach(FileUtils::deleteFile);
                }
            }

        // 2. 清理不再需要的旧附件
        List<NoticeAttachmentVo> oldAttachments = noticeAttachmentMapper.getByNoticeId(noticeId);
        if (oldAttachments != null && !oldAttachments.isEmpty()) {
            List<Long> attachmentsToDeleteIds = oldAttachments.stream()
                    .map(NoticeAttachmentVo::getAttachmentId)
                    .filter(id -> keepAttachmentIds == null || !keepAttachmentIds.contains(id))
                    .collect(Collectors.toList());
            
            if (!attachmentsToDeleteIds.isEmpty()) {
                List<String> attachmentFilesToDelete = attachmentsToDeleteIds.stream()
                        .map(id -> noticeAttachmentMapper.getAttachmentById(id).getFilePath())
                .collect(Collectors.toList());
                noticeAttachmentMapper.deleteBatch(attachmentsToDeleteIds);
                attachmentFilesToDelete.forEach(FileUtils::deleteFile);
            }
        }
        
        // 3. 插入新上传的图片
        if (newImages != null && !newImages.isEmpty()) {
            for (NoticeImage image : newImages) {
                image.setNoticeId(noticeId);
                noticeImageMapper.insert(image);
            }
        }
        
        // 4. 插入新上传的附件
        if (newAttachments != null && !newAttachments.isEmpty()) {
            for (NoticeAttachment attachment : newAttachments) {
                attachment.setNoticeId(noticeId);
                noticeAttachmentMapper.insert(attachment);
            }
        }

        // 5. 更新公告主信息
        noticeDto.setUpdateTime(LocalDateTime.now());
        noticeMapper.update(noticeDto);
        
        // 如果公告是新发布的或者内容有更新，创建通知
        if (isPublishedNow && (!wasPublished || !oldNotice.getTitle().equals(noticeDto.getTitle()) || !oldNotice.getContent().equals(noticeDto.getContent()))) {
            try {
                // 确定通知重要性
                Integer importance = noticeDto.getType() == 0 ? 1 : 0; // 通知类型为0时(系统通知)设为重要
                
                // 创建通知
                NotificationUtils.createAnnouncementNotification(
                    noticeDto.getTitle(),
                    noticeDto.getContent(),
                    noticeId,
                    userId,
                    importance
                );
                log.info("成功为更新的公告ID: {} 创建系统通知", noticeId);
            } catch (Exception e) {
                log.error("为更新的公告创建通知失败: {}", e.getMessage(), e);
                // 通知创建失败不影响公告发布
            }
        }
        
        clearNoticeCache();
    }
    
    /**
     * 清除公告相关缓存
     */
    private void clearNoticeCache() {
        // 清除公告列表缓存
        Set<String> listKeys = redisTemplate.keys("notice:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
        }
        
        // 清除公告详情缓存
        Set<String> detailKeys = redisTemplate.keys("notice:detail:*");
        if (detailKeys != null && !detailKeys.isEmpty()) {
            redisTemplate.delete(detailKeys);
        }
        
        // 清除首页公告缓存
        redisTemplate.delete("notice:recent");
        redisTemplate.delete("notice:recentActivity");
        redisTemplate.delete("notice:allRecent");
    }
    
    /**
     * 更新公告状态
     * @param noticeStatusUpdateDto 状态更新信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(NoticeStatusUpdateDto noticeStatusUpdateDto) {
        Long noticeId = noticeStatusUpdateDto.getNoticeId();
        Integer newStatus = noticeStatusUpdateDto.getStatus();
        
        // 记录操作用户信息
        Long currentUserId = UserContextHolder.getCurrentId();
        log.info("用户 {} 尝试更新公告状态，公告ID: {}, 目标状态: {}", 
                currentUserId, noticeId, newStatus);
        
        if (noticeId == null || newStatus == null) {
            log.error("公告ID或状态不能为空");
            throw new IllegalArgumentException("公告ID或状态不能为空");
        }
        
        // 验证状态值是否有效（0-草稿，1-已发布）
        if (newStatus != 0 && newStatus != 1) {
            log.error("无效的公告状态: {}", newStatus);
            throw new IllegalArgumentException("公告状态只能为0(草稿)或1(已发布)");
        }
        
        // 获取原公告信息
        NoticeVo originalNotice = noticeMapper.getById(noticeId);
        if (originalNotice == null) {
            log.error("公告不存在，ID: {}", noticeId);
            throw new IllegalArgumentException("公告不存在");
        }
        
        log.info("更新公告状态，公告ID: {}, 原状态: {}, 新状态: {}", 
                noticeId, originalNotice.getStatus(), newStatus);
        
        // 如果状态相同，直接返回
        if (originalNotice.getStatus().equals(newStatus)) {
            log.info("公告状态未发生变化，无需更新");
            return;
        }
        
        // 准备更新参数
        String publisherName = null;
        LocalDateTime publishTime = null;
        
        // 如果状态变为已发布，需要设置发布时间和发布者
        if (newStatus == 1) {
            publishTime = LocalDateTime.now();
            
            // 获取当前用户信息作为发布者
            Long userId = UserContextHolder.getCurrentId();
            publisherName = "系统"; // 默认值
            
            if (userId != null) {
                User user = userMapper.getUserById(userId);
                if (user != null && StringUtils.hasText(user.getName())) {
                    publisherName = user.getName();
                }
            }
            
            log.info("公告发布，发布者: {}, 发布时间: {}", publisherName, publishTime);
        }
        
        // 使用专门的状态更新方法
        noticeMapper.updateStatus(noticeId, newStatus, publisherName, publishTime);
        
        // 如果公告变为已发布状态，创建系统通知
        if (newStatus == 1) {
            try {
                Long userId = UserContextHolder.getCurrentId();
                Integer importance = originalNotice.getType() == 0 ? 1 : 0; // 系统通知为重要
                
                NotificationUtils.createAnnouncementNotification(
                    originalNotice.getTitle(),
                    originalNotice.getContent(),
                    noticeId,
                    userId,
                    importance
                );
                log.info("成功为公告状态更新创建系统通知，公告ID: {}", noticeId);
            } catch (Exception e) {
                log.error("为公告状态更新创建通知失败: {}", e.getMessage(), e);
                // 通知创建失败不影响状态更新
            }
        }
        
        // 清除缓存
        clearNoticeCache();
        
        log.info("公告状态更新成功，ID: {}, 新状态: {}", noticeId, newStatus);
    }
}

