package com.noyes.jogakbo.album.DTO;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumInfo {

  private String albumUUID;
  private String albumName;
  private String thumbnailImageURL;
  private LocalDateTime createdDate;
  private LocalDateTime lastModifiedDate;
}
