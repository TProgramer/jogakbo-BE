package com.noyes.jogakbo.album.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumInfo {

  private String albumUUID;
  private String albumName;
  private String thumbnailImageURL;
  private LocalDateTime createdDate;
  private LocalDateTime lastModifiedDate;
}
