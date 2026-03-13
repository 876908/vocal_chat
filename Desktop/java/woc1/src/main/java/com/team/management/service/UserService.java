package com.team.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.team.management.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;


public interface UserService extends IService<User>, UserDetailsService {
    String login(String username, String password);
    @Override
    org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username);
}
