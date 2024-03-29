package com.noyes.jogakbo.global.login;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import com.noyes.jogakbo.global.jwt.JwtService;
import com.noyes.jogakbo.user.UserRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtService jwtService;
  private final UserRepository userRepository;

  @SuppressWarnings("null")
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {

    String userUUID = extractUsername(authentication); // 인증 정보에서 Username(userUUID) 추출
    String accessToken = jwtService.createAccessToken(userUUID); // JwtService의 createAccessToken을 사용하여 AccessToken 발급
    String refreshToken = jwtService.createRefreshToken(); // JwtService의 createRefreshToken을 사용하여 RefreshToken 발급

    // 응답 헤더에 AccessToken, RefreshToken 실어서 응답
    jwtService.sendAccessAndRefreshToken(response, accessToken, refreshToken);

    userRepository.findById(userUUID)
        .ifPresent(user -> {
          user.updateRefreshToken(refreshToken);
          userRepository.save(user);
        });
  }

  private String extractUsername(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    return userDetails.getUsername();
  }
}