package com.example.splitdata.controller;

import com.example.splitdata.entity.Course;
import com.example.splitdata.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Date 2022/3/1 15:55
 * @Description
 */
@RestController
public class CourseController {
    @Resource
    private CourseService courseService;

    @GetMapping("/courses")
    public Object list() {
        return courseService.list();
    }

    @PostMapping("/add")
    public Object add(String name, Long userId) {
        Course course = Course.builder().cname(name).userId(userId).status("NORMAL").build();
        return courseService.add(course);
    }
}
