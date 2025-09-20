package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * security 用户详情实现类
 */

public class UserDetailsImpl  implements UserDetails {
    //定义一个USer
    private User user;
    //GrantedAuthority 是一个security的权限列表类
    private List<GrantedAuthority> permissionsList;

    //一个构造方法 传入用户和权限列表
    public UserDetailsImpl(User user,List<String> permissionsList) {
        //当构造函数的参数名（如 user）与类的成员变量名相同时，Java 会默认使用局部参数（而非成员变量),使用 this.user 明确指定要赋值的是当前对象的成员变量。
        this.user=user;

        //将权限字符串转化为GrantedAuthority列表,permissionsList.stream()转为流
        this.permissionsList=permissionsList.stream()
                .map(SimpleGrantedAuthority::new) // 将每个角色转换为 SimpleGrantedAuthority
                .collect(Collectors.toList()); // 将流收集为列表
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissionsList;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // 默认账户未过期
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // 默认账户未锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // 默认凭证未过期
    }

    @Override
    public boolean isEnabled() {
        return true;  // 默认账户启用
    }
}
