package com.example.splitdata.service;

import com.example.splitdata.entity.Course;

import java.util.List;

/**
 * @Date 2022/3/1 15:52
 * @Description
 */
public interface CourseService {
    Course add(Course course);
    List<Course> list();
}
