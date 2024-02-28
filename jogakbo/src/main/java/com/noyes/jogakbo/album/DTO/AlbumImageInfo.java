package com.noyes.jogakbo.album.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlbumImageInfo {

  private String albumImageUUID;
  private AlbumImageSizeInfo size;
  private AlbumImageLocationInfo location;
  private double rotation;
}
