package com.noyes.jogakbo.album.DTO;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntryMessage {

  private String albumName;
  private List<List<ImagesInPage>> imagesInfo;
  private String thumbnailImage;
  private LocalDateTime createdDate;
}
