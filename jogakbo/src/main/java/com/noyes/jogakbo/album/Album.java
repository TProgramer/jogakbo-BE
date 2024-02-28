package com.noyes.jogakbo.album;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.noyes.jogakbo.album.DTO.AlbumImageInfo;

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
  private List<List<AlbumImageInfo>> images;
  private String albumOwner;
  // field for album co-workers list
  private List<String> albumEditors;
  // field for invited user list
  private List<String> sentAlbumInvitations;

  @Version
  private int version;

  @CreatedDate
  private LocalDateTime createdDate;

  @LastModifiedDate
  private LocalDateTime lastModifiedDate;
}
