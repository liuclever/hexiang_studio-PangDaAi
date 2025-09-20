package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.dv.vo.StudioInfoVo;
import com.back_hexiang_studio.entity.StudioInfo;
import com.back_hexiang_studio.enumeration.OperationType;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.StudioInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工作室信息控制器
 * 查看权限：STUDIO_INFO_VIEW（所有用户）
 * 修改权限：ROLE_ADMIN 或 ROLE_MANAGER（管理员和主任）
 */
@RestController
@RequestMapping("/admin/studio")
@Slf4j
@PreAuthorize("hasAuthority('STUDIO_INFO_VIEW')")
public class StudioInfoController {

    @Autowired
    private StudioInfoService studioInfoService;
    
    /**
     * 获取工作室信息
     * @return 工作室信息
     */
    @GetMapping("/info")
    public Result<StudioInfoVo> getStudioInfo() {
        StudioInfoVo studioInfo = studioInfoService.getStudioInfo();
        return Result.success(studioInfo);
    }
    
    /**
     * 更新工作室信息
     * @param studioInfo 工作室信息
     * @return 结果
     */
    @PostMapping("/update")
    @AutoFill(OperationType.UPDATE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    public Result updateStudioInfo(@RequestBody StudioInfo studioInfo) {
        log.info("更新工作室信息，操作用户ID: {}", studioInfo.getUpdateUser());
        studioInfoService.updateStudioInfo(studioInfo);
        return Result.success();
    }
} 