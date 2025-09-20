package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.dv.dto.DepartmentDto;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@RestController
@RequestMapping("/admin/department")
@Slf4j
@CrossOrigin
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取部门列表
     * @return 部门列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result getDepartmentList() {
        log.info("获取部门列表");
        try {
            List<?> departments = departmentService.getAllDepartments();
            return Result.success(departments);
        } catch (Exception e) {
            log.error("获取部门列表失败", e);
            return Result.error("获取部门列表失败: " + e.getMessage());
        }
    }

    /**
     * 新增部门
     * @param departmentDto 部门信息
     * @return 操作结果
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result addDepartment(@RequestBody DepartmentDto departmentDto) {
        log.info("新增部门: {}", departmentDto.getDepartmentName());
        try {
            if (departmentDto.getDepartmentName() == null || departmentDto.getDepartmentName().trim().isEmpty()) {
                return Result.error("部门名称不能为空");
            }

            departmentService.addDepartment(departmentDto);
            log.info("部门新增成功: {}", departmentDto.getDepartmentName());
            return Result.success("部门新增成功");
        } catch (Exception e) {
            log.error("新增部门失败", e);
            return Result.error("新增部门失败: " + e.getMessage());
        }
    }

    /**
     * 更新部门
     * @param departmentDto 部门信息
     * @return 操作结果
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result updateDepartment(@RequestBody DepartmentDto departmentDto) {
                log.info("更新部门: ID={}, 名称={}", departmentDto.getId(), departmentDto.getDepartmentName());
        try {
            if (departmentDto.getId() == null) {
                return Result.error("部门ID不能为空");
            }
            
            if (departmentDto.getDepartmentName() == null || departmentDto.getDepartmentName().trim().isEmpty()) {
                return Result.error("部门名称不能为空");
            }
            
            departmentService.updateDepartment(departmentDto);
            log.info("部门更新成功: ID={}, 名称={}", departmentDto.getId(), departmentDto.getDepartmentName());
            return Result.success("部门更新成功");
        } catch (Exception e) {
            log.error("更新部门失败", e);
            return Result.error("更新部门失败: " + e.getMessage());
        }
    }

    /**
     * 删除部门
     * @param ids 部门ID列表
     * @return 操作结果
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
    public Result deleteDepartments(@RequestBody List<Long> ids) {
        log.info("删除部门: IDs={}", ids);
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的部门");
            }

            departmentService.deleteDepartments(ids);
            log.info("部门删除成功: IDs={}", ids);
            return Result.success("部门删除成功");
        } catch (Exception e) {
            log.error("删除部门失败", e);
            return Result.error("删除部门失败: " + e.getMessage());
        }
    }
} 