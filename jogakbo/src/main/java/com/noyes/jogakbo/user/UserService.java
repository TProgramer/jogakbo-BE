package com.noyes.jogakbo.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noyes.jogakbo.album.Album;
import com.noyes.jogakbo.global.jwt.JwtService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final JwtService jwtService;

  /**
   * User 생성
   * JPA Repository의 save Method를 사용하여 객체를 생성
   * Entity인 Model 객체에 @Id로 설정한 키 값이 없을 경우 해당하는 데이터를 추가
   * 만약 추가하려는 Entity인 Model 객체에 @Id 값이 이미 존재하면 갱신되기 때문에
   * 아래와 같이 추가하고자 하는 User가 존재하는지 체크하는 로직을 추가
   * 
   * @param model
   * @return
   */
  public void signUp(UserSignUpDTO userSignUpDto) throws Exception {

    if (userRepository.findById(userSignUpDto.getSocialId()).isPresent()) {
      throw new Exception("이미 존재하는 이메일입니다.");
    }

    User user = User.builder()
        .socialID(userSignUpDto.getSocialId())
        .nickname(userSignUpDto.getNickname())
        .role(Role.USER)
        .build();

    userRepository.save(user);
  }

  public Optional<User> checkUser(HttpServletResponse response, String accessToken) {

    String socialID = jwtService.extractSocialId(accessToken).get();
    Optional<User> targetUser = userRepository.findById(socialID);

    if (targetUser.isPresent())
      return updateToken(response, socialID, targetUser);

    return registUser(response, socialID, accessToken);
  }

  public Optional<User> updateToken(HttpServletResponse response, String socialID, Optional<User> targetUser) {

    String newAccessToken = jwtService.createAccessToken(socialID);
    String newRefreshToken = jwtService.createRefreshToken();

    // 응답 헤더에 AccessToken, RefreshToken 실어서 응답
    jwtService.sendAccessAndRefreshToken(response, newAccessToken, newRefreshToken);

    User user = targetUser.get();
    user.updateRefreshToken(newRefreshToken);
    userRepository.save(user);
    log.info("=============================================");
    log.info("로그인에 성공하였습니다. socialID : {}", socialID);
    log.info("AccessToken : {}", newAccessToken);

    return targetUser;
  }

  public Optional<User> registUser(HttpServletResponse response, String socialID, String accessToken) {

    String newAccessToken = jwtService.createAccessToken(socialID);
    String newRefreshToken = jwtService.createRefreshToken();

    // 응답 헤더에 AccessToken, RefreshToken 실어서 응답
    jwtService.sendAccessAndRefreshToken(response, newAccessToken, newRefreshToken);

    String nickname = jwtService.extractName(accessToken).get();
    String provider = jwtService.extractProvider(accessToken).get();

    User user = User.builder()
        .socialID(socialID)
        .nickname(nickname)
        .provider(provider)
        .albums(new ArrayList<>())
        .friends(new ArrayList<>())
        .role(Role.USER)
        .build();

    user.updateRefreshToken(newRefreshToken);
    userRepository.save(user);
    log.info("=============================================");
    log.info("로그인에 성공하였습니다. socialID : {}", socialID);
    log.info("AccessToken : {}", newAccessToken);

    return Optional.ofNullable(user);
  }

  /**
   * <pre>
   * Refresh Token을 재발급하고 DB에 Refresh Token을 업데이트하는 메소드
   * jwtService.createRefreshToken()으로 Refresh Token을 재발급 후
   * DB에 재발급한 Refresh Token 업데이트 후 Flush
   * </pre>
   */
  public void reIssueRefreshToken(HttpServletResponse response, String refreshToken) {

    userRepository.findByRefreshToken(refreshToken)
        .ifPresent(user -> {
          String reIssuedRefreshToken = jwtService.createRefreshToken();
          user.updateRefreshToken(reIssuedRefreshToken);
          userRepository.save(user);
          jwtService.sendAccessAndRefreshToken(response, jwtService.createAccessToken(user.getSocialID()),
              reIssuedRefreshToken);
        });
  }

  public void deleteRefreshToken(HttpServletResponse response, String socialID) {

    Optional<User> targetUser = getUser(socialID);

    if (!targetUser.isPresent()) {

      log.info("잘못된 요청값");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

      return;
    }

    User user = targetUser.get();
    user.updateRefreshToken(null);
    userRepository.save(user);
    log.info("리프레시 토큰 삭제 완료!");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  public Album getAlbumByUser(String socialID, String albumID) {

    List<Album> albums = userRepository.findById(socialID).get().getAlbums();
    for (Album album : albums) {

      if (album.getAlbumID().equals(albumID))
        return album;
    }

    return null;
  }

  public List<Album> getAlbumsByUser(String socialID) {

    return userRepository.findById(socialID).get().getAlbums();
  }

  public void addAlbum(Album newAlbum, String socialID) {

    User targetUser = userRepository.findById(socialID).get();
    List<Album> albums = targetUser.getAlbums();
    albums.add(newAlbum);
    userRepository.save(targetUser);
  }

  /**
   * User 수정
   * JPA Repository의 save Method를 사용하여 객체를 갱신
   * Entity인 Model 객체에 @Id로 설정한 키 값이 존재할 경우 해당하는 데이터를 갱신
   * 만약 수정하려는 Entity인 Model 객체에 @Id 값이 존재하지 않으면 데이터가 추가되기 때문에
   * 아래와 같이 갱신하고자 하는 User가 존재하는지 체크하는 로직을 추가
   *
   * @param model
   * @return
   */
  public User updateUserInfo(UserDetails token, UserUpdateDTO updateData) {

    User updatedUser = null;

    try {

      if (updateData.isUserUpdateEmpty())
        throw new Exception("Required info is not qualified");

      User existUser = getUser(token.getUsername()).get();

      existUser.setNickname(updateData.getNickname());

      // if (!ObjectUtils.isEmpty(existUser))
      // updatedUser = userRepository.save(model);

    } catch (Exception e) {

      log.info("[Fail] e: " + e.toString());
    }

    return updatedUser;
  }

  /**
   * User List 조회
   * JPA Repository의 findAll Method를 사용하여 전체 User를 조회
   * 
   * @return
   */
  public List<User> getUsers() {
    return userRepository.findAll();
  }

  /**
   * Id에 해당하는 User 조회
   * JPA Repository의 findBy Method를 사용하여 특정 User를 조회
   * find 메소드는 NULL 값일 수도 있으므로 Optional<T>를 반환하지만,
   * Optional 객체의 get() 메소드를 통해 Entity로 변환해서 반환함.
   * 
   * @param id
   * @return
   */
  public Optional<User> getUser(String socialID) {

    return userRepository.findById(socialID);
  }

  /**
   * Id에 해당하는 User 삭제
   * JPA Repository의 deleteBy Method를 사용하여 특정 User를 삭제
   * 
   * @param id
   */
  public void deleteUser(String socialID) {
    userRepository.deleteBySocialID(socialID);
  }
}
