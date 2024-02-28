package com.noyes.jogakbo.album.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AlbumImageInfo {

  private int page;
  private ImageSize size;
  private AlbumImageLocationInfo location;
  private double rotation;
}
