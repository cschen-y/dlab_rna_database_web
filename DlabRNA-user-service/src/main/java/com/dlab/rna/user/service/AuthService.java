package com.dlab.rna.user.service;

import com.dlab.rna.user.model.LoginResponse;
import com.dlab.rna.user.model.User;

/**
 * 认证服务接口
 */
public interface AuthService {
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录响应
     */
    LoginResponse login(String username, String password);
    
    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param phone 手机号
     * @return 注册的用户
     */
    User register(String username, String password, String email, String phone);
}