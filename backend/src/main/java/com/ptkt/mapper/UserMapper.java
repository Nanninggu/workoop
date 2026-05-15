package com.ptkt.mapper;

import com.ptkt.model.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    void insert(User user);
}
