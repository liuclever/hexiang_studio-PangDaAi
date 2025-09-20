package com.back_hexiang_studio.dv.vo;

import lombok.Data;

@Data
public class StudioInfoVo {
    /** 主键ID */
    private Integer id;
    
    /** 工作室名称 */
    private String name;
    
    /** 成立时间 */
    private String establishTime;
    
    /** 负责人 */
    private String director;
    
    /** 成员数量 */
    private Integer memberCount;
    
    /** 项目数量 */
    private Integer projectCount;
    
    /** 获奖情况 */
    private String awards;
    
    /** 联系电话 */
    private String phone;
    
    /** 联系邮箱 */
    private String email;
    
    /** 地址 */
    private String address;
    
    /** 具体房间号 */
    private String room;
} 