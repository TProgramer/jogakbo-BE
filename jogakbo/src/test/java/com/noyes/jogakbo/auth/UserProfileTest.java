package com.noyes.jogakbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import com.noyes.jogakbo.global.AcceptanceTest;
import com.noyes.jogakbo.global.jwt.JwtService;
import com.noyes.jogakbo.user.Role;
import com.noyes.jogakbo.user.DTO.UserProfile;

@AcceptanceTest
@DisplayName("로그인 테스트")
public class UserProfileTest {

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private JwtService jwtService;

  @Value("${jwt.secretKey}")
  private String secretKey;

  @Value("${jwt.access.header}")
  private String accessHeader;

  @SuppressWarnings("null")
  @Nested
  @DisplayName("Given: 신규 유저가 로그인 한 상태에서")
  class login_request_from_beginner {

    String testToken = jwtService.createLoginTestToken("Beginner_UUID", "Beginner_Test");

    String beginnerAccessToken = webTestClient
        .post()
        .uri("/login")
        .header(accessHeader, "Bearer " + testToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody(UserProfile.class)
        .returnResult()
        .getResponseHeaders()
        .get(accessHeader)
        .get(0);

    @Nested
    @DisplayName("When: 프로필 정보를 요청하면")
    class request_beginner_user_profile {

      ResponseSpec response = webTestClient
          .get()
          .uri("/user")
          .accept(MediaType.APPLICATION_JSON)
          .header(accessHeader, "Bearer " + beginnerAccessToken)
          .exchange();

      @Test
      @DisplayName("Then: Role 이 BEGINNER 인 UserProfile이 반환된다.")
      void response_UserProfile_with_BEGINNER_Role() {

        UserProfile beginnerUserProfile = response
            .expectStatus().isOk()
            .expectBody(UserProfile.class)
            .returnResult()
            .getResponseBody();

        assertEquals(Role.BEGINNER, beginnerUserProfile.getRole());
      }
    }
  }
}
