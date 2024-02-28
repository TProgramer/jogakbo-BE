package com.noyes.jogakbo.album.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AlbumImageEditInfo {

  private int pageNum;
  private AlbumImageSizeInfo size;
  private AlbumImageLocationInfo location;
  private double rotation;
}
