package com.noyes.jogakbo.album;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.noyes.jogakbo.album.DTO.AlbumImageEditMessage;
import com.noyes.jogakbo.album.DTO.AlbumDetailInfo;
import com.noyes.jogakbo.album.DTO.AlbumEntryInfo;
import com.noyes.jogakbo.album.DTO.AlbumInitInfo;
import com.noyes.jogakbo.album.DTO.AlbumInvitationMessage;
import com.noyes.jogakbo.album.DTO.AlbumImageInfo;
import com.noyes.jogakbo.album.DTO.AlbumMemberInfo;
import com.noyes.jogakbo.global.SseEmitters;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/album")
@Tag(name = "앨범", description = "앨범 작업 API 모음")
public class AlbumController {

  private final AlbumService albumService;
  private final SimpMessagingTemplate simpMessageTemplate;
  private final SseEmitters sseEmitters;

  @Operation(description = "앨범 생성 API입니다.")
  @PostMapping()
  public ResponseEntity<String> createAlbum(@RequestParam String albumName, Principal principal) {

    String newAlbumID = albumService.createAlbum(albumName, principal.getName());
    log.info(newAlbumID);

    return ResponseEntity.ok(newAlbumID);
  }

  @Operation(description = "입장하려는 앨범 정보 제공 API입니다.")
  @GetMapping("/{albumUUID}")
  public ResponseEntity<AlbumEntryInfo> getAlbumEntryInfo(@PathVariable String albumUUID, Principal principal) {

    return ResponseEntity.ok(albumService.getAlbumEntryInfo(principal.getName(), albumUUID));
  }

  @Operation(description = "앨범 입장 후, 앨범 상태 초기화 API입니다.")
  @GetMapping("/{albumUUID}/init")
  public ResponseEntity<AlbumInitInfo> getAlbumInfo(@PathVariable String albumUUID, Principal principal) {

    return ResponseEntity.ok(albumService.getEntryMessage(principal.getName(), albumUUID));
  }

  @SuppressWarnings("null")
  @Operation(description = "앨범 페이지 추가 API입니다.")
  @PostMapping("{albumUUID}/page")
  public ResponseEntity<String> createPage(@PathVariable String albumUUID, Principal principal)
      throws JsonMappingException {

    List<List<AlbumImageInfo>> imagesInfo = albumService.addNewPage(albumUUID);
    simpMessageTemplate.convertAndSend("/sub/edit/" + albumUUID, imagesInfo);
    return ResponseEntity.ok("페이지를 성공적으로 추가했습니다.");
  }

  @SuppressWarnings("null")
  @Operation(description = "앨범 내부 사진 등록 API입니다.")
  @PostMapping("/{albumUUID}/image")
  public ResponseEntity<String> uploadImages(@PathVariable String albumUUID,
      @RequestPart List<MultipartFile> multipartFiles,
      @RequestParam String fileInfos) {

    List<List<AlbumImageInfo>> imagesInfo = albumService.uploadImages(albumUUID, multipartFiles, fileInfos);
    simpMessageTemplate.convertAndSend("/sub/edit/" + albumUUID, imagesInfo);

    return ResponseEntity.ok("사진을 성공적으로 등록했습니다.");
  }

  @SuppressWarnings("null")
  @Operation(description = "앨범 내부 사진 삭제 API입니다.")
  @DeleteMapping("/{albumUUID}/image/{albumImageUUID}")
  public ResponseEntity<String> unloadImage(@PathVariable String albumUUID,
      @PathVariable String albumImageUUID) {

    List<List<AlbumImageInfo>> imagesInfo = albumService.unloadImage(albumUUID, albumImageUUID);
    simpMessageTemplate.convertAndSend("/sub/edit/" + albumUUID, imagesInfo);

    return ResponseEntity.ok("이미지를 성공적으로 제외했습니다.");
  }

  @Operation(description = "공동 작업을 위한 웹소켓 API 입니다.")
  @MessageMapping("/edit/{albumUUID}")
  @SendTo("/sub/edit/{albumUUID}")
  public List<List<AlbumImageInfo>> editImage(@DestinationVariable String albumUUID,
      List<AlbumImageEditMessage> payload)
      throws Exception {

    return albumService.editImage(albumUUID, payload);
  }

  @Operation(description = "앨범 정보 변경 API 입니다.")
  @PutMapping("/{albumUUID}")
  public ResponseEntity<String> updateProfile(@PathVariable String albumUUID, @RequestParam String newAlbumName,
      @RequestPart(required = false) MultipartFile thumbnailImage,
      Principal principal) {

    String result = albumService.updateProfile(albumUUID, newAlbumName, thumbnailImage, principal.getName());

    return ResponseEntity.ok(result);
  }

  @Operation(description = "앨범 삭제 API입니다.")
  @DeleteMapping("/{albumUUID}")
  public ResponseEntity<String> removeAlbum(@PathVariable String albumUUID, Principal principal) throws IOException {

    String result = albumService.removeAlbum(albumUUID, principal.getName());

    return ResponseEntity.ok(result);
  }

  @Operation(description = "앨범 초대 API입니다.")
  @PostMapping("/{albumUUID}/invitation/{collaboUserUUID}")
  public ResponseEntity<String> sendAlbumInvitation(@PathVariable String albumUUID,
      @PathVariable String collaboUserUUID,
      Principal principal) {

    AlbumInvitationMessage albumInvitationMessage = albumService.sendAlbumInvitation(albumUUID, collaboUserUUID,
        principal.getName());

    if (albumInvitationMessage == null)
      return ResponseEntity.ok("이미 앨범에 초대했거나 요청을 보낸 상대입니다.");

    String result = sseEmitters.sendAlbumInvitation(collaboUserUUID, albumInvitationMessage);

    return ResponseEntity.ok(result);
  }

  @Operation(description = "앨범 초대 응답 API입니다. reply 파라미터의 값이 accept 면 앨범에 참여하게 됩니다.")
  @PostMapping("/{albumUUID}/invitation-reply")
  public ResponseEntity<String> replyAlbumInvitation(@PathVariable String albumUUID, @RequestParam String reply,
      Principal principal) {

    String res = albumService.replyAlbumInvitation(albumUUID, principal.getName(), reply);

    return ResponseEntity.ok(res);
  }

  @Operation(description = "앨범 구성원 조회 API입니다.")
  @GetMapping("/{albumUUID}/album-member-info")
  public ResponseEntity<AlbumMemberInfo> getAlbumMemberInfo(@PathVariable String albumUUID,
      Principal principal) {

    AlbumMemberInfo albumMemberInfo = albumService.getAlbumMemberInfo(albumUUID, principal.getName());

    return ResponseEntity.ok(albumMemberInfo);
  }

  @Operation(description = "앨범 상세 정보 조회 API입니다.")
  @GetMapping("/{albumUUID}/album-detail-info")
  public ResponseEntity<AlbumDetailInfo> getAlbumDetailInfo(@PathVariable String albumUUID,
      Principal principal) {

    AlbumDetailInfo albumMemberInfo = albumService.getAlbumDetailInfo(albumUUID, principal.getName());

    return ResponseEntity.ok(albumMemberInfo);
  }
}