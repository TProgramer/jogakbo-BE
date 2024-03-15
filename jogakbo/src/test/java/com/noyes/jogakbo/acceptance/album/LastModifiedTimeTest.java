package com.noyes.jogakbo.acceptance.album;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.noyes.jogakbo.acceptance.global.AcceptanceTest;
import com.noyes.jogakbo.album.DTO.AlbumInfo;
import com.noyes.jogakbo.global.jwt.JwtService;
import com.noyes.jogakbo.user.DTO.UserProfile;

@AcceptanceTest
@DisplayName("앨범 최근 수정일 테스트")
public class LastModifiedTimeTest {

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  JwtService jwtService;

  @Value("${jwt.secretKey}")
  private String secretKey;

  @Value("${jwt.access.header}")
  private String accessHeader;

  @SuppressWarnings("null")
  @Nested
  @DisplayName("Given: 유저가 앨범을 생성한 상태에서")
  class authenicated_user {

    String testToken = jwtService.createLoginTestToken("User_UUID", "LastModifiedTime_Test");

    String accessToken = webTestClient
        .post()
        .uri("/login")
        .header(accessHeader, "Bearer " + testToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody(UserProfile.class)
        .returnResult()
        .getResponseHeaders()
        .get(accessHeader).get(0);

    String albumUUID = webTestClient
        .post()
        .uri("/album?albumName=LastModifiedTestAlbum")
        .accept(MediaType.TEXT_PLAIN)
        .header(accessHeader, "Bearer " + testToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();

    @Nested
    @DisplayName("앨범을 편집하면")
    class request_for_profile_with_AlbumInfo {

      String addPageResult = webTestClient
          .post()
          .uri("/album/" + albumUUID + "/page")
          .accept(MediaType.TEXT_PLAIN)
          .header(accessHeader, "Bearer " + testToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody(String.class)
          .returnResult()
          .getResponseBody();

      LocalDateTime requestTime = LocalDateTime.now();

      @Test
      @DisplayName("프로필 요청 시, 최근 수정시간 정보를 반영한 AlbumInfo를 반환한다.")
      void response_with_last_modified_date_in_AlbumInfo() {

        UserProfile userProfile = webTestClient
            .get()
            .uri("/user")
            .accept(MediaType.APPLICATION_JSON)
            .header(accessHeader, "Bearer " + accessToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserProfile.class)
            .returnResult()
            .getResponseBody();

        AlbumInfo albumInfo = userProfile.getAlbums().stream()
            .filter(album -> album.getAlbumUUID().equals(albumUUID)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException());

        LocalDateTime lastModifiedTime = albumInfo.getLastModifiedDate();
        Duration duration = Duration.between(lastModifiedTime, requestTime);
        assertTrue(duration.toSeconds() < 1);
      }
    }
  }
}
