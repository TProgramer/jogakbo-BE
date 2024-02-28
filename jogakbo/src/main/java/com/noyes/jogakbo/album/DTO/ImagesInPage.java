package com.noyes.jogakbo.album.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImagesInPage {
  // 메시지 타입 : 입장, 채팅, 나감
  // public enum MessageType {
  // ENTER, EDIT, TALK, QUIT
  // }

  // private MessageType type; // 메시지 타입
  private String imageUUID;
  private ImageSize size;
  private AlbumImageLocationInfo location;
  private double rotation;
}
