package com.noyes.jogakbo.user;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserSignUpDTO {

  private String userUUID;
  private String nickname;
  private int age;
}