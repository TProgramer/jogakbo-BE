package com.noyes.jogakbo.global.websocket;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.noyes.jogakbo.global.jwt.PasswordUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Component
@RequiredArgsConstructor
public class FilterChannelInterceptor implements ChannelInterceptor {

  private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);

    assert headerAccessor != null;
    if (headerAccessor.getCommand() == StompCommand.CONNECT) {
      String token = String.valueOf(headerAccessor.getNativeHeader("Authorization").get(0));
      log.info("token 값 확인 : " + token);
    }

    String password = PasswordUtil.generateRandomPassword();

    UserDetails userDetailsUser = User.builder()
        .username("myUser.getSocialId()")
        .password(password)
        // .roles(myUser.getRole().name())
        .roles("USER")
        .build();

    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetailsUser, null,
        authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    headerAccessor.setUser(authentication);
    return message;
  }
}