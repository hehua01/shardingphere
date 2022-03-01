package com.example.sharding.controller;

import com.example.sharding.entity.User;
import com.example.sharding.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Date 2022/2/25 18:00
 * @Description
 */
@RestController
public class UserController {
    @Resource
    private UserService userService;

    @GetMapping("/users")
    public Object list() {
        return userService.list();
    }

    @PostMapping("/add")
    public Object add(String name, String city) {
        User user = new User();
        user.setCity(city);
        user.setName(name);
        return userService.addUser(user);
    }
}
