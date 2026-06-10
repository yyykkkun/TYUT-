package com.tyut.mall.service;

import com.tyut.mall.dto.request.AnonymousLoginRequest;
import com.tyut.mall.dto.request.LoginRequest;
import com.tyut.mall.dto.request.RegisterRequest;
import com.tyut.mall.dto.response.LoginVO;
import com.tyut.mall.dto.response.UserVO;

public interface AuthService {
    LoginVO login(LoginRequest request);
    LoginVO register(RegisterRequest request);
    LoginVO anonymousLogin(AnonymousLoginRequest request);
    UserVO me(Long userId);
}
