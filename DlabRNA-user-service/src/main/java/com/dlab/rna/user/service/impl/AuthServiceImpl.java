package com.dlab.rna.user.service.impl;

import com.dlab.rna.user.mapper.UserMapper;
import com.dlab.rna.user.model.LoginResponse;
import com.dlab.rna.user.model.User;
import com.dlab.rna.user.service.AuthService;
import com.dlab.rna.user.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Date;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(String username, String password) {
        LoginResponse response = new LoginResponse();
        
        // 根据用户名查询用户
        User user = userMapper.findByUsername(username);
        
        if (user == null) {
            response.setMessage("用户不存在");
            return response;
        }
        
        // 验证密码（支持BCrypt哈希，并对历史明文进行迁移）
        boolean isPasswordValid = false;
        String storedPassword = user.getPassword();
        boolean hasEncoder = passwordEncoder != null;
        boolean storedIsBCrypt = storedPassword != null && (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"));
        
        if (hasEncoder) {
            if (storedIsBCrypt) {
                isPasswordValid = passwordEncoder.matches(password, storedPassword);
            } else {
                // 明文历史：先比较明文，成功后迁移为哈希
                isPasswordValid = password.equals(storedPassword);
                if (isPasswordValid) {
                    user.setPassword(passwordEncoder.encode(password));
                    user.setUpdateTime(new Date());
                    userMapper.update(user);
                }
            }
        } else {
            // 无编码器（不推荐）：仅明文比较
            isPasswordValid = password.equals(storedPassword);
        }
        
        if (!isPasswordValid) {
            response.setMessage("密码错误");
            return response;
        }
        
        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUsername());
        
        // 设置响应信息
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setUserId(user.getId());
        response.setMessage("登录成功");
        
        return response;
    }
    
    @Override
    public User register(String username, String password, String email, String phone) {
        // 检查用户名是否已存在
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        
        // 如果配置了密码编码器，则加密密码
        if (passwordEncoder != null) {
            user.setPassword(passwordEncoder.encode(password));
        } else {
            user.setPassword(password);
        }
        
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1); // 默认启用
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        
        // 保存用户并返回实体（useGeneratedKeys 将填充 user.id）
        userMapper.save(user);
        return user;
    }
}