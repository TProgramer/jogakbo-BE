package com.noyes.jogakbo.user;

import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.noyes.jogakbo.global.SseEmitters;
import com.noyes.jogakbo.user.DTO.Friend;
import com.noyes.jogakbo.user.DTO.FriendSearchResult;
import com.noyes.jogakbo.user.DTO.UserProfile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user")
@RestController
@Tag(name = "유저", description = "유저 관련 API 입니다.")
public class UserController {

  private final UserService userService;
  private final SseEmitters sseEmitters;

  /**
   * Member 생성
   *
   * @return
   * @throws ParseException
   */
  @Operation(description = "유저 등록 메서드입니다.")
  @PostMapping("/sign-up")
  public ResponseEntity<String> createUser(@RequestBody UserSignUpDTO userSignUpDto) throws Exception {

    userService.signUp(userSignUpDto);

    return ResponseEntity.ok("회원가입 성공!");
  }

  /**
   * User 정보 수정
   * 
   * @AuthenticationPrincipal 를 통해 인증정보를 반아오기
   *
   * @return
   * @throws ParseException
   */
  @Operation(description = "유저 정보 수정 메서드입니다.")
  @PutMapping()
  public ResponseEntity<User> updateUser(@AuthenticationPrincipal UserDetails token,
      @RequestBody UserUpdateDTO updateData) throws ParseException {

    User updatedUser = userService.updateUserInfo(token, updateData);

    if (!ObjectUtils.isEmpty(updatedUser)) {

      return new ResponseEntity<>(updatedUser, HttpStatus.OK);

    } else {

      return new ResponseEntity<>(updatedUser, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Member List 조회
   *
   * @return
   */
  @Operation(description = "유저 전체 조회 메서드입니다.")
  @GetMapping("/list")
  public ResponseEntity<List<User>> getUsers() {

    List<User> users = userService.getUsers();

    return new ResponseEntity<>(users, HttpStatus.OK);
  }

  @Operation(description = "본인 프로필 조회 메서드입니다.")
  @GetMapping("/profile")
  public ResponseEntity<UserProfile> getProfile(Principal principal) {

    String socialID = principal.getName();
    UserProfile userProfile = userService.getUserProfile(socialID);

    return ResponseEntity.ok(userProfile);
  }

  @Operation(description = "닉네임으로 친구 찾기 메서드입니다.")
  @GetMapping("/search")
  public ResponseEntity<List<FriendSearchResult>> searchFriend(@RequestParam String nickname, Principal principal) {

    if (nickname.equals(""))
      return ResponseEntity.ok(List.of());

    List<FriendSearchResult> searchResult = userService.searchFriend(nickname, principal.getName());

    return ResponseEntity.ok(searchResult);
  }

  @Operation(description = "친구 추가 요청 메서드입니다.")
  @PostMapping("/friend")
  public ResponseEntity<String> sendFriendRequest(@RequestParam String socialID, Principal principal) {

    Friend requestUser = userService.sendFriendRequest(principal.getName(), socialID);

    if (requestUser == null)
      return ResponseEntity.ok("이미 친구이거나 요청을 보낸 상대입니다.");

    String result = sseEmitters.sendFriendRequestAlarm(socialID, requestUser);

    return ResponseEntity.ok(result);
  }

  @Operation(description = "실시간 알림을 위한 SSE 등록 엔드포인트입니다.")
  @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> connect(Principal principal) {

    SseEmitter emitter = new SseEmitter(60 * 1000L);

    sseEmitters.add(principal.getName(), emitter);

    try {

      emitter.send(SseEmitter.event()
          .name("connect")
          .data("connected!"));

    } catch (IOException e) {

      throw new RuntimeException(e);
    }

    return ResponseEntity.ok(emitter);
  }

  @Operation(description = "친구 요청 응답 엔드포인트입니다. reply 파라미터의 값이 accept 면 친구가 됩니다. ")
  @PostMapping("/friend-reply")
  public ResponseEntity<String> replyFriendRequest(@RequestParam String socialID, @RequestParam String reply,
      Principal principal) {

    String res = userService.replyFriendRequest(socialID, principal.getName(), reply);

    return ResponseEntity.ok(res);
  }

  @Operation(description = "친구 삭제 엔드포인트입니다.")
  @DeleteMapping("/friend")
  public ResponseEntity<String> removeFriend(@RequestParam String socialID,
      Principal principal) {

    String res = userService.removeFriend(socialID, principal.getName());
    log.debug(res);

    return ResponseEntity.ok(res);
  }

  @Operation(description = "유저의 프로필 변경 API 입니다.")
  @PutMapping("profile")
  public ResponseEntity<String> updateProfile(@RequestParam String newNickname,
      @RequestPart(required = false) MultipartFile profileImage,
      Principal principal) {

    String result = userService.updateProfile(newNickname, profileImage, principal.getName());

    return ResponseEntity.ok(result);
  }
}