package com.back_hexiang_studio.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "password")
    private String password;

    @Column(name = "name")
    private String name;

    @Column(name = "sex")
    private String sex;

    @Column(name = "phone")
    private String phone;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "createUser")
    private Long createUser;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "updateUser")
    private Long updateUser;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "status")
    private String status;

    @Column(name = "position_id")
    private Long positionId;

    @Column(name = "email")
    private String email;




}
