package com.noyes.jogakbo.user;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.noyes.jogakbo.album.Album;
import com.noyes.jogakbo.user.DTO.Friend;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "user")
public class User {

  @Id
  private String socialID;
  private Timestamp regDate;
  private Role role;
  private String nickname;
  private String provider;
  private String profileImageUrl;
  private String refreshToken;
  private List<Album> albums;
  private List<Friend> friends;

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