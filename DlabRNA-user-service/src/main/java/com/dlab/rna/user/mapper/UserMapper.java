package com.dlab.rna.user.mapper;

import com.dlab.rna.user.model.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户仓库接口
 */
@Mapper
public interface UserMapper {
    /**
     * 根据ID查找用户
     */
    User findById(Long id);
    
    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 保存用户
     */
    int save(User user);

    /**
     * 更新用户
     */
    int update(User user);

    /**
     * 根据ID删除用户
     */
    void deleteById(Long id);
}