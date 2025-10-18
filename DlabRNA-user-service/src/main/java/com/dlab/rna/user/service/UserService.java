package com.dlab.rna.user.service;

import com.dlab.rna.user.model.User;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 根据ID获取用户
     */
    User getUserById(Long id);

    /**
     * 创建用户
     */
    User createUser(User user);

    /**
     * 更新用户
     */
    User updateUser(User user);

    /**
     * 删除用户
     */
    void deleteUser(Long id);
}