package com.noyes.jogakbo.album.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ImageInfo {

  private int page;
  private ImageSize size;
  private ImageLocation location;
  private double rotation;
}
