package com.noyes.jogakbo.global.login;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.noyes.jogakbo.global.jwt.PasswordUtil;
import com.noyes.jogakbo.user.User;
import com.noyes.jogakbo.user.UserService;

@Service
@RequiredArgsConstructor
public class LoginService implements UserDetailsService {

  private final UserService userService;

  @Override
  public UserDetails loadUserByUsername(String userUUID) throws UsernameNotFoundException {
    User user = userService.getUser(userUUID)
        .orElseThrow(() -> new UsernameNotFoundException("해당 소셜 ID가 존재하지 않습니다."));

    String password = PasswordUtil.generateRandomPassword();
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUserUUID())
        .password(password)
        .roles(user.getRole().name())
        .build();
  }
}