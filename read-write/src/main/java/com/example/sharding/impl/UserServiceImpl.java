package com.example.sharding.impl;

import com.example.sharding.entity.User;
import com.example.sharding.repository.UserRepo;
import com.example.sharding.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Date 2022/2/25 17:58
 * @Description
 */
@Service
public class UserServiceImpl implements UserService {
    @Resource
    UserRepo userRepository;

    @Override
    public User addUser(User user) {

        // 强制路由主库
//        HintManager.getInstance().setMasterRouteOnly();
        return userRepository.save(user);
    }

    @Override
    public List<User> list() {
        return userRepository.findAll();
    }
}
