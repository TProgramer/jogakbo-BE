package com.noyes.jogakbo.user;

import java.time.LocalDateTime;
import java.util.List;

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
  private Role role;
  private String nickname;
  private String provider;
  private String profileImageOriginalName;
  private String profileImageUrl;
  private String refreshToken;
  private List<String> friends;
  private List<String> friendRequestees;
  private List<String> friendRequesters;
  // field for collaboration albums list
  private List<String> collaboAlbums;
  // field for recieved invitation from album owner
  private List<String> albumInviters;
  private List<String> albums;

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