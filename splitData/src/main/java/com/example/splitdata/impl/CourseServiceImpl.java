package com.example.splitdata.impl;

import com.example.splitdata.entity.Course;
import com.example.splitdata.repository.CourseRepo;
import com.example.splitdata.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Date 2022/3/1 15:54
 * @Description
 */
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepo courseRepo;

    @Override
    public Course add(Course course) {
        return courseRepo.save(course);
    }

    @Override
    public List<Course> list() {
        return courseRepo.findAll();
    }
}
