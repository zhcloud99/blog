package com.zhcloud.blog.service;

import com.zhcloud.blog.po.User;

public interface UserService {
    User checkUser(String username, String password);
}
