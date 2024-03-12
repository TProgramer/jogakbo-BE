package com.noyes.jogakbo.album;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

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

  // field with randomUUID for unique key
  @Id
  private String albumUUID;
  private String albumName;
  // field with userUUID in String value
  private String albumOwner;
  // field presents image path in AWS S3
  private String thumbnailImageURL;
  // field to recognize image update
  @Builder.Default
  private String thumbnailOriginalName = "";
  @Builder.Default
  private List<List<AlbumImageInfo>> albumImages = List.of(new ArrayList<>());
  // field for album co-workers list
  @Builder.Default
  private List<String> albumEditors = new ArrayList<>();
  // field for invited user list
  @Builder.Default
  private List<String> albumInvitees = new ArrayList<>();

  // check flag to use @CreatedDate, @LastModified annotation with custom PK
  @Version
  private int version;

  @CreatedDate
  private LocalDateTime createdDate;

  @LastModifiedDate
  private LocalDateTime lastModifiedDate;
}
