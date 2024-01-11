package com.noyes.jogakbo.album;

import java.util.List;

import com.noyes.jogakbo.album.DTO.ImagesInPage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Album {

  private String albumID;

  private String albumName;

  private List<List<ImagesInPage>> images;

  private List<String> albumEditors;
}
