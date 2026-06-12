package com.tyut.mall.service.impl;

import com.tyut.mall.common.ApiException;
import com.tyut.mall.dto.request.AnonymousLoginRequest;
import com.tyut.mall.dto.request.LoginRequest;
import com.tyut.mall.dto.request.RegisterRequest;
import com.tyut.mall.dto.response.LoginVO;
import com.tyut.mall.dto.response.UserVO;
import com.tyut.mall.entity.User;
import com.tyut.mall.repository.UserRepository;
import com.tyut.mall.security.JwtUtil;
import com.tyut.mall.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginRequest request) {
        // 支持邮箱或手机号登录
        Optional<User> userOpt = userRepository.findByUsername(request.getAccount());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(request.getAccount());
        }
        User user = userOpt.orElseThrow(() -> ApiException.badRequest("账号或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.badRequest("账号或密码错误");
        }
        if (user.getStatus() == 0) {
            throw ApiException.badRequest("账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginVO.builder()
                .token(token)
                .user(toUserVO(user))
                .build();
    }

    @Override
    public LoginVO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ApiException.badRequest("用户名已存在");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .level("普通会员")
                .balance(BigDecimal.ZERO)
                .points(0)
                .giftCard(BigDecimal.ZERO)
                .growth(0)
                .status(1)
                .build();
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginVO.builder()
                .token(token)
                .user(toUserVO(user))
                .build();
    }

    @Override
    public LoginVO anonymousLogin(AnonymousLoginRequest request) {
        // 用手机号查找已有用户，没有则自动注册
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> {
                    String nickname = "匿名用户" + request.getPhone().substring(Math.max(0, request.getPhone().length() - 4));
                    User newUser = User.builder()
                            .username("u_" + request.getPhone())
                            .password(passwordEncoder.encode("123456"))
                            .phone(request.getPhone())
                            .nickname(nickname)
                            .level("普通会员")
                            .balance(BigDecimal.ZERO)
                            .points(0)
                            .giftCard(BigDecimal.ZERO)
                            .growth(0)
                            .status(1)
                            .build();
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginVO.builder()
                .token(token)
                .user(toUserVO(user))
                .build();
    }

    @Override
    public UserVO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("用户不存在"));
        return toUserVO(user);
    }

    private UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(String.valueOf(user.getId()))
                .name(user.getNickname())
                .account(user.getUsername())
                .level(user.getLevel())
                .role(user.getRole())
                .build();
    }
}
