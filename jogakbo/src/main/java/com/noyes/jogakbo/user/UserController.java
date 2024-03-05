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
@Tag(name = "유저", description = "유저 API 모음집")
public class UserController {

  private final MyPageService myPageService;
  private final UserService userService;
  private final SseEmitters sseEmitters;

  @Operation(description = "본인 프로필 조회 API입니다.")
  @GetMapping()
  public ResponseEntity<UserProfile> getProfile(Principal principal) {

    String userUUID = principal.getName();
    UserProfile userProfile = myPageService.getUserProfile(userUUID);

    return ResponseEntity.ok(userProfile);
  }

  @Operation(description = "닉네임으로 친구 찾기 API입니다.")
  @GetMapping("/search")
  public ResponseEntity<List<FriendSearchResult>> searchFriend(@RequestParam String nickname, Principal principal) {

    if (nickname.equals(""))
      return ResponseEntity.ok(List.of());

    List<FriendSearchResult> searchResult = userService.searchFriend(nickname, principal.getName());

    return ResponseEntity.ok(searchResult);
  }

  @Operation(description = "친구 추가 요청 API입니다.")
  @PostMapping("/friend-request/{userUUID}")
  public ResponseEntity<String> sendFriendRequest(@PathVariable String userUUID, Principal principal) {

    Friend requestUser = userService.sendFriendRequest(principal.getName(), userUUID);

    if (requestUser == null)
      return ResponseEntity.ok("이미 친구이거나 요청을 보낸 상대입니다.");

    String result = sseEmitters.sendFriendRequestAlarm(userUUID, requestUser);

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

  @Operation(description = "친구 요청 응답 API입니다. reply 파라미터의 값이 accept 면 친구가 됩니다. ")
  @PostMapping("/friend-reply/{userUUID}")
  public ResponseEntity<String> replyFriendRequest(@PathVariable String userUUID, @RequestParam String reply,
      Principal principal) {

    String res = userService.replyFriendRequest(userUUID, principal.getName(), reply);

    return ResponseEntity.ok(res);
  }

  @Operation(description = "친구 삭제 엔드포인트입니다.")
  @DeleteMapping("/friend/{userUUID}")
  public ResponseEntity<String> removeFriend(@PathVariable String userUUID,
      Principal principal) {

    String res = userService.removeFriend(userUUID, principal.getName());
    log.debug(res);

    return ResponseEntity.ok(res);
  }

  @Operation(description = "본인 프로필 수정 API 입니다.")
  @PutMapping()
  public ResponseEntity<String> updateProfile(@RequestParam String newNickname,
      @RequestPart(required = false) MultipartFile profileImage,
      Principal principal) {

    String result = userService.updateProfile(newNickname, profileImage, principal.getName());

    return ResponseEntity.ok(result);
  }
}