package com.example.splitdata.repository;

import com.example.splitdata.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Date 2022/3/1 15:51
 * @Description
 */
@Repository
public interface CourseRepo extends JpaRepository<Course, Long> {
}
