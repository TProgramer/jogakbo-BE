package com.noyes.jogakbo.user;

import java.sql.Timestamp;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Document(collection = "user")
@Builder
@Data
public class UserDocument {

  @Id
  private String socialId;
  private Timestamp regDate;
  private Role role;
  private String nickname;
  private String profileImageUrl;
  private String refreshToken;

  // 유저 권한 승격 메소드
  public void authorizeUser() {
    this.role = Role.USER;
  }

  public void updateRefreshToken(String newRefreshToken) {
    this.refreshToken = newRefreshToken;
  }
}