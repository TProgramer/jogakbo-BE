package com.noyes.jogakbo.user;

import java.sql.Date;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.NONE)
public class UserUpdateDTO {

  private String nickname;
  private Date birthDay;
  private String phoneNum;

  public boolean isUserUpdateEmpty() {

    return Stream.of(nickname, birthDay, phoneNum)
        .anyMatch(Objects::isNull);
  }
}