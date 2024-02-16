package com.noyes.jogakbo.global.websocket;

import org.springframework.web.socket.WebSocketSession;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebSocketSessionInfo {

  private WebSocketSession session;
  private String destination;
}
