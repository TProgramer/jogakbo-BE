package com.noyes.jogakbo.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "user")
public class User {

  @Id
  private String userUUID;
  private String nickname;
  private String provider;
  private String refreshToken;
  private String profileImageUrl;
  @Builder.Default
  private String profileImageOriginalName = "";
  @Builder.Default
  private Role role = Role.USER;
  @Builder.Default
  private List<String> friends = new ArrayList<>();
  @Builder.Default
  private List<String> friendRequestees = new ArrayList<>();
  @Builder.Default
  private List<String> friendRequesters = new ArrayList<>();
  // field for collaboration albums list
  @Builder.Default
  private List<String> collaboAlbums = new ArrayList<>();
  // field for recieved invitation from album owner
  @Builder.Default
  private List<String> albumInviters = new ArrayList<>();
  @Builder.Default
  private List<String> albums = new ArrayList<>();

  // check flag to use @CreatedDate, @LastModified annotation with custom PK
  @Version
  private int version;

  @CreatedDate
  private LocalDateTime createdDate;

  @LastModifiedDate
  private LocalDateTime lastModifiedDate;

  // 유저 권한 승격 메소드
  public void authorizeUser() {
    this.role = Role.USER;
  }

  public void updateRefreshToken(String newRefreshToken) {
    this.refreshToken = newRefreshToken;
  }

  public Object ifPresent(Object object) {
    return null;
  }
}