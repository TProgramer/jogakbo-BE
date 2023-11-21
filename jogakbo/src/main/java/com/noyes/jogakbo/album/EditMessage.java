package com.noyes.jogakbo.album;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditMessage {
  // 메시지 타입 : 입장, 채팅, 나감
  public enum MessageType {
    ENTER, EDIT, TALK, QUIT
  }

  private MessageType type; // 메시지 타입
  private String albumId; // 방번호
  private String sender; // 메시지 보낸사람
  private String message; // 메시지
}
