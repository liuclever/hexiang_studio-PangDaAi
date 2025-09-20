package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.dv.vo.SupportContactVo;
import com.back_hexiang_studio.entity.SupportContact;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.SupportContactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 技术支持控制器
 * 权限：所有登录用户都可以访问
 */
@Slf4j
@RestController
@RequestMapping("/admin/support")
@PreAuthorize("hasAuthority('DASHBOARD_VIEW')") // 基本权限，所有登录用户都有
public class SupportController {

    @Autowired
    private SupportContactService supportContactService;

    /**
     * 获取所有活跃的技术支持联系人
     * @return 技术支持联系人列表
     */
    @GetMapping("/contacts")
    public Result<List<SupportContactVo>> getAllContacts() {
        log.info("获取技术支持联系人列表");
        List<SupportContact> contacts = supportContactService.getAllActiveContacts();
        
        // 转换为VO对象
        List<SupportContactVo> contactVos = new ArrayList<>();
        for (SupportContact contact : contacts) {
            SupportContactVo vo = new SupportContactVo();
            BeanUtils.copyProperties(contact, vo);
            contactVos.add(vo);
        }
        
        return Result.success(contactVos);
    }
} 