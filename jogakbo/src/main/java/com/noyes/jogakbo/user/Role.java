package com.noyes.jogakbo.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

  GUEST("ROLE_GUEST"), BEGINNER("ROLE_BEGINNER"), USER("ROLE_USER"), ADMIN("ROLE_ADMIN");

  private final String key;
}
