package com.noyes.jogakbo.global;

import com.noyes.jogakbo.global.jwt.JwtAuthenticationProcessingFilter;
import com.noyes.jogakbo.global.jwt.JwtService;
import com.noyes.jogakbo.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

/**
 * <pre>
* 인증은 CustomJsonUsernamePasswordAuthenticationFilter에서 authenticate()로 인증된
사용자로 처리
* JwtAuthenticationProcessingFilter는 AccessToken, RefreshToken 재발급
 * </pre>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CorsConfig corsConfig;
  private final JwtService jwtService;
  private final UserService userService;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.logout()
        .logoutUrl("/logout") // 로그아웃 처리 URL (= form action url)
        .addLogoutHandler((request, response, authentication) -> {
          log.info("로그아웃 호출");
          // TODO - 로그아웃 당시에 토큰이 만료됐을 경우를 추가 고려
          jwtService.extractAccessToken(request)
              .filter(jwtService::isTokenValid)
              .ifPresentOrElse(accessToken -> jwtService.extractUserUUID(accessToken)
                  .ifPresent(userUUID -> userService.deleteRefreshToken(response, userUUID)),
                  () -> {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                  });
        })
        // 로그아웃 성공 핸들러
        .logoutSuccessHandler((request, response, authentication) -> {
          // 기본 핸들러의 redirect 요청을 막기위해 사용
        });

    http
        .formLogin().disable() // FormLogin 사용 안함
        .httpBasic().disable() // httpBasic 사용 안함
        .csrf().disable() // Token 기반 인증 방식이기에 csrf 보안 사용 안함
        .headers().frameOptions().disable() // Graphiql 접근을 위해 Click Jacking 공격을 막는 X-Frame-Options 비활성화
        .and()
        // 세션을 사용하지 않으므로 STATELESS로 설정
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        // URL별 권한 관리 옵션
        .authorizeRequests()
        // 아이콘, css, js 관련
        // 기본 페이지, css, image, js 하위 폴더에 있는 자료들은 모두 접근 가능, graphiql에도 추가로 접근 가능
        .antMatchers("/css/**", "/images/**", "/js/**", "/favicon.ico", "/v3/api-docs/**", "/swagger-ui/**", "/logout",
            "/album-ws")
        .permitAll()
        .anyRequest().authenticated(); // 위의 경로 이외에는 모두 인증된 사용자만 접근 가능

    // 원래 스프링 시큐리티 필터 순서가 LogoutFilter 이후에 로그인 필터 동작
    // 따라서, LogoutFilter 이후에 커스텀 필터가 동작하도록 설정
    // 순서 : LogoutFilter -> JwtAuthenticationProcessingFilter
    http.addFilter(corsConfig.corsFilter()); // ** CorsFilter 등록 **
    http.addFilterAfter(jwtAuthenticationProcessingFilter(), LogoutFilter.class);

    return http.build();
  }

  @Bean
  public JwtAuthenticationProcessingFilter jwtAuthenticationProcessingFilter() {

    JwtAuthenticationProcessingFilter jwtAuthenticationFilter = new JwtAuthenticationProcessingFilter(jwtService,
        userService);

    return jwtAuthenticationFilter;
  }
}
