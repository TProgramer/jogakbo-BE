package com.noyes.jogakbo.global;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {

    // http.logout()
    // .logoutUrl("/logout") // 로그아웃 처리 URL (= form action url)
    // // .logoutSuccessUrl("/login") // 로그아웃 성공 후 targetUrl,
    // // logoutSuccessHandler 가 있다면 효과 없으므로 주석처리.
    // .addLogoutHandler((request, response, authentication) -> {
    // // 사실 굳이 내가 세션 무효화하지 않아도 됨.
    // // LogoutFilter가 내부적으로 해줌.
    // HttpSession session = request.getSession();
    // if (session != null) {
    // session.invalidate();
    // }
    // }) // 로그아웃 핸들러 추가
    // .logoutSuccessHandler((request, response, authentication) -> {
    // response.sendRedirect("/login");
    // }) // 로그아웃 성공 핸들러
    // .deleteCookies("remember-me"); // 로그아웃 후 삭제할 쿠키 지정

    return http
        .csrf(csrf -> csrf.disable()) // (1)
        .authorizeRequests(auth -> {
          auth.anyRequest().authenticated(); // (2)
        })
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // (3)
        .httpBasic(withDefaults()) // (4)
        .build();
  }

  @Bean
  public InMemoryUserDetailsManager userDetailsManager() {

    UserDetails user = User.withDefaultPasswordEncoder() // (1)
        .username("user")
        .password("password")
        .roles("USER")
        .build();

    UserDetails admin = User.withDefaultPasswordEncoder() // (2)
        .username("admin")
        .password("password")
        .roles("USER", "ADMIN")
        .build();

    return new InMemoryUserDetailsManager(user, admin); // (3)
  }
}

// import com.noyes.jogakbo.global.jwt.JwtAuthenticationProcessingFilter;
// import com.noyes.jogakbo.global.jwt.JwtService;
// import com.noyes.jogakbo.user.UserRepository;

// import lombok.RequiredArgsConstructor;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import
// org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import
// org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.crypto.factory.PasswordEncoderFactories;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.logout.LogoutFilter;

// /**
// * <pre>
// * 인증은 CustomJsonUsernamePasswordAuthenticationFilter에서 authenticate()로 인증된
// 사용자로 처리
// * JwtAuthenticationProcessingFilter는 AccessToken, RefreshToken 재발급
// * </pre>
// */
// @Configuration
// @EnableWebSecurity
// @RequiredArgsConstructor
// public class SecurityConfig {

// private final JwtService jwtService;
// private final UserRepository userRepository;

// @Bean
// public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
// http
// .formLogin().disable() // FormLogin 사용 안함
// .httpBasic().disable() // httpBasic 사용 안함
// .csrf().disable() // Token 기반 인증 방식이기에 csrf 보안 사용 안함
// .headers().frameOptions().disable() // Graphiql 접근을 위해 Click Jacking 공격을 막는
// X-Frame-Options 비활성화
// .and()

// // 세션을 사용하지 않으므로 STATELESS로 설정
// .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
// .and()

// // URL별 권한 관리 옵션
// .authorizeRequests()
// // 아이콘, css, js 관련
// // 기본 페이지, css, image, js 하위 폴더에 있는 자료들은 모두 접근 가능, graphiql에도 추가로 접근 가능
// .antMatchers("/css/**", "/images/**", "/js/**", "/favicon.ico", "/graphiql")
// .permitAll()
// .anyRequest().authenticated(); // 위의 경로 이외에는 모두 인증된 사용자만 접근 가능

// // 원래 스프링 시큐리티 필터 순서가 LogoutFilter 이후에 로그인 필터 동작
// // 따라서, LogoutFilter 이후에 커스텀 필터가 동작하도록 설정
// // 순서 : LogoutFilter -> JwtAuthenticationProcessingFilter
// http.addFilterAfter(jwtAuthenticationProcessingFilter(), LogoutFilter.class);

// return http.build();
// }

// @Bean
// public PasswordEncoder passwordEncoder() {

// return PasswordEncoderFactories.createDelegatingPasswordEncoder();
// }

// @Bean
// public JwtAuthenticationProcessingFilter jwtAuthenticationProcessingFilter()
// {

// JwtAuthenticationProcessingFilter jwtAuthenticationFilter = new
// JwtAuthenticationProcessingFilter(jwtService,
// userRepository);

// return jwtAuthenticationFilter;
// }
// }
