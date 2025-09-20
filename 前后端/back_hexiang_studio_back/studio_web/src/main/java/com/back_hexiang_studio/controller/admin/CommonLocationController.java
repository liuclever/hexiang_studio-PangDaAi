package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.dv.dto.CommonLocationDto;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.CommonLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常用签到地点控制器
 * 权限：超级管理员或只有副主任、主任可以访问（属于考勤管理）
 */
@RestController
@RequestMapping("/admin/locations")
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ATTENDANCE_MANAGE')")
public class CommonLocationController {
    
    @Autowired
    private CommonLocationService commonLocationService;
    
    /**
     * 获取所有常用签到地点
     * @return 常用地点列表
     */
    @GetMapping
    public Result<List<CommonLocationDto>> getAllCommonLocations() {
        List<CommonLocationDto> locations = commonLocationService.getAllCommonLocations();
        return Result.success(locations);
    }
    
    /**
     * 根据ID获取常用签到地点
     * @param id 地点ID
     * @return 常用地点
     */
    @GetMapping("/{id}")
    public Result<CommonLocationDto> getCommonLocationById(@PathVariable Integer id) {
        CommonLocationDto location = commonLocationService.getCommonLocationById(id);
        return Result.success(location);
    }
    
    /**
     * 新增常用签到地点
     * @param locationDto 常用地点数据
     * @return 新增的地点ID
     */
    @PostMapping
    public Result<Integer> createCommonLocation(@RequestBody CommonLocationDto locationDto) {
        Integer id = commonLocationService.createCommonLocation(locationDto);
        return Result.success(id);
    }
    
    /**
     * 更新常用签到地点
     * @param id 地点ID
     * @param locationDto 常用地点数据
     * @return 结果
     */
    @PutMapping("/{id}")
    public Result updateCommonLocation(@PathVariable Integer id, @RequestBody CommonLocationDto locationDto) {
        commonLocationService.updateCommonLocation(id, locationDto);
        return Result.success();
    }
    
    /**
     * 删除常用签到地点
     * @param id 地点ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    public Result deleteCommonLocation(@PathVariable Integer id) {
        commonLocationService.deleteCommonLocation(id);
        return Result.success();
    }
} 