package com.dlab.rna.user.service.impl;

import com.dlab.rna.user.model.User;
import com.dlab.rna.user.mapper.UserMapper;
import com.dlab.rna.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getUserById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User createUser(User user) {
        userMapper.save(user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        userMapper.update(user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}