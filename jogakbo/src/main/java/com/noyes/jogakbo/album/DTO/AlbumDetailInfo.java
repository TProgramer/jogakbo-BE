package com.noyes.jogakbo.album.DTO;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumDetailInfo {

  private String albumName;
  private String thumbnailImage;
  private LocalDateTime createdDate;
  private boolean isPublic;
}
