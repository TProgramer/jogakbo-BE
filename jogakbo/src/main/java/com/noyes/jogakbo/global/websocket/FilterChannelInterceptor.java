package com.noyes.jogakbo.global.websocket;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.noyes.jogakbo.album.AlbumService;
import com.noyes.jogakbo.global.jwt.JwtService;
import com.noyes.jogakbo.global.jwt.PasswordUtil;

import lombok.RequiredArgsConstructor;

@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Component
@RequiredArgsConstructor
public class FilterChannelInterceptor implements ChannelInterceptor {

  private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
  private final JwtService jwtService;
  private final AlbumService albumService;

  @SuppressWarnings("null")
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {

    StompHeaderAccessor headerAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    assert headerAccessor != null;
    if (headerAccessor.getCommand() == StompCommand.CONNECT) {

      // 담겨있는 유저 userUUID가 유효한 지 검증 후 추출
      String token = String.valueOf(headerAccessor.getNativeHeader("Authorization").get(0));
      String userUUID = jwtService.extractUserUUID(token.replace("Bearer ", ""))
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유저 인증에 실패하였습니다."));

      // 확인된 userUUID 를 기반으로 세션 유저 설정
      String password = PasswordUtil.generateRandomPassword();

      UserDetails userDetailsUser = User.builder()
          .username(userUUID)
          .password(password)
          .roles("USER")
          .build();

      Authentication authentication = new UsernamePasswordAuthenticationToken(userDetailsUser, null,
          authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities()));

      SecurityContextHolder.getContext().setAuthentication(authentication);
      headerAccessor.setUser(authentication);

    } else if (headerAccessor.getCommand() == StompCommand.SUBSCRIBE) {

      // headerAccessor 에서 sessionID와 albumID, 유저 userUUID 추출
      String sessionID = headerAccessor.getSessionId();
      String albumID = headerAccessor.getDestination().split("/")[3];
      String userUUID = headerAccessor.getUser().getName();

      // 유저가 albumEditors 에 포함되어 있는지 검증
      if (!albumService.isValidAlbumEditor(albumID, userUUID))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 존재하지 않습니다.");

      // sessionID에 구독 albumID를 경로를 업데이트
      WebSocketSessionHolder.updateSessionWithDestination(sessionID, albumID);

    } else if (headerAccessor.getCommand() == StompCommand.DISCONNECT) {

      // 관리 대상에서 특정 sessionID 제거
      String sessionID = headerAccessor.getSessionId();
      WebSocketSessionHolder.removeSession(sessionID);
    }

    return message;
  }
}