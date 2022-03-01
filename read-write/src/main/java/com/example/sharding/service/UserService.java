package com.example.sharding.service;

import com.example.sharding.entity.User;

import java.util.List;

/**
 * @Date 2022/2/25 17:57
 * @Description
 */
public interface UserService {
    User addUser(User user);
    List<User> list();
}
