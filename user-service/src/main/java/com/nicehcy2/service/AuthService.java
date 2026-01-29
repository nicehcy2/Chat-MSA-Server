package com.nicehcy2.service;

import com.nicehcy2.common.util.JwtUtil;
import com.nicehcy2.dto.CustomUserInfoDto;
import com.nicehcy2.dto.LoginRequestDto;
import com.nicehcy2.dto.SignupRequestDto;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public String login(LoginRequestDto requestDto) {
        String email = requestDto.email();
        String password = requestDto.password();
        User user = userRepository.findUserByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("이메일이 존재하지 않습니다.");
        }

        if (!encoder.matches(password, user.getPassword())) {

            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        CustomUserInfoDto info = CustomUserInfoDto.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .password(user.getPassword())
                .build();

        return jwtUtil.createAccessToken(info);
    }

    public Long signup(SignupRequestDto signupRequestDto) {

        User exist = userRepository.findUserByEmail(signupRequestDto.email());
        if (exist != null) {
            throw new RuntimeException("이메일이 이미 존재합니다");
        }

        String encodePassword = encoder.encode(signupRequestDto.password());

        User user = User.builder()
                .nickname(signupRequestDto.nickname())
                .password(encodePassword)
                .gender(signupRequestDto.gender())
                .userRole(signupRequestDto.userRole())
                .email(signupRequestDto.email())
                .imageUrl(signupRequestDto.imageUrl())
                .build();

        User saved = userRepository.save(user);
        return saved.getUserId();
    }
}
