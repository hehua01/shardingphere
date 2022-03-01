package com.example.sharding.repository;

import com.example.sharding.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Date 2022/2/25 17:56
 * @Description
 */
@Repository
public interface UserRepo extends JpaRepository<User, Long> {
}
