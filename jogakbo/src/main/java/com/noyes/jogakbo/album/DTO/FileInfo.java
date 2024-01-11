package com.noyes.jogakbo.album.DTO;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileInfo {
  // 메시지 타입 : 입장, 채팅, 나감
  // public enum MessageType {
  // ENTER, EDIT, TALK, QUIT
  // }

  // private MessageType type; // 메시지 타입
  private List<ImageInfo> imageInfo; // 메시지
}