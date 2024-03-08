package com.noyes.jogakbo.album.DTO;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumEntryInfo {

  private String albumUUID;
  private String albumName;
  private String thumbnailImageURL;
  private LocalDateTime createdDate;
  private int memberCount;
  private int imageCount;
}
