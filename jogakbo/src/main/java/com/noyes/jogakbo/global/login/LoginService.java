package com.noyes.jogakbo.global.login;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.noyes.jogakbo.global.jwt.PasswordUtil;
import com.noyes.jogakbo.user.UserDocument;
import com.noyes.jogakbo.user.UserRepository;

@Service
@RequiredArgsConstructor
public class LoginService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String socialId) throws UsernameNotFoundException {
    UserDocument user = userRepository.findBySocialId(socialId)
        .orElseThrow(() -> new UsernameNotFoundException("해당 소셜 ID가 존재하지 않습니다."));

    String password = PasswordUtil.generateRandomPassword();
    return User.builder()
        .username(user.getSocialId())
        .password(password)
        .roles(user.getRole().name())
        .build();
  }
}