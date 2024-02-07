package com.noyes.jogakbo.album;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.noyes.jogakbo.album.DTO.ImagesInPage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "album")
public class Album {

  @Id
  private String albumID;

  private String albumName;

  private String thumbnailImage;
  private String thumbnailOriginalName;

  private List<List<ImagesInPage>> images;

  private String albumOwner;

  private List<String> albumEditors;

  @Version
  private int version;

  @CreatedDate
  private LocalDateTime createdDate;

  @LastModifiedDate
  private LocalDateTime lastModifiedDate;
}
