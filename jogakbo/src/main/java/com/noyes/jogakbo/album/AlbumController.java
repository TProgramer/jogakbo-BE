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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.noyes.jogakbo.album.DTO.EditMessage;
import com.noyes.jogakbo.album.DTO.EntryMessage;
import com.noyes.jogakbo.album.DTO.ImagesInPage;
import com.noyes.jogakbo.global.SseEmitters;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/album")
@Tag(name = "앨범", description = "앨범 작업 관련 API 입니다.")
public class AlbumController {

  private final AlbumService albumService;
  private final SimpMessagingTemplate simpMessageTemplate;
  private final SseEmitters sseEmitters;

  @Operation(description = "앨범 등록 메서드입니다.")
  @PostMapping()
  public ResponseEntity<String> createAlbum(@RequestParam String albumName, Principal principal)
      throws JsonProcessingException {

    String newAlbumID = albumService.createAlbum(albumName, principal.getName());
    log.info(newAlbumID);

    return ResponseEntity.ok(newAlbumID);
  }

  @Operation(description = "특정 앨범 정보 조회 메서드입니다.")
  @GetMapping()
  public ResponseEntity<EntryMessage> getAlbumInfo(@RequestParam String albumID, Principal principal)
      throws JsonProcessingException {

    return ResponseEntity.ok(albumService.getEntryMessage(principal.getName(), albumID));
  }

  @Operation(description = "유저 소유의 전체 앨범 조회 메서드입니다.")
  @GetMapping("/list")
  public ResponseEntity<List<Album>> album(Principal principal) {

    List<Album> albums = albumService.getAllAlbumByUser(principal.getName());
    return ResponseEntity.ok(albums);
  }

  @Operation(description = "앨범 페이지 추가 메서드입니다.")
  @PostMapping("/newPage/{albumID}")
  public ResponseEntity<String> createPage(@PathVariable String albumID, Principal principal)
      throws JsonMappingException, JsonProcessingException {

    List<List<ImagesInPage>> imagesInfo = albumService.addNewPage(albumID);
    simpMessageTemplate.convertAndSend("/sub/edit/" + albumID, imagesInfo);
    return ResponseEntity.ok("페이지를 성공적으로 추가했습니다.");
  }

  @Operation(description = "앨범 내부 사진 등록 메서드입니다.")
  @PostMapping("/img/{albumID}")
  public ResponseEntity<String> uploadImages(@PathVariable String albumID,
      @RequestPart List<MultipartFile> multipartFiles,
      @RequestParam String fileInfos) throws JsonProcessingException {

    List<List<ImagesInPage>> imagesInfo = albumService.uploadImages(albumID, multipartFiles, fileInfos);
    simpMessageTemplate.convertAndSend("/sub/edit/" + albumID, imagesInfo);

    return ResponseEntity.ok("사진을 성공적으로 등록했습니다.");
  }

  @Operation(description = "앨범 내부 사진 삭제 메서드입니다.")
  @DeleteMapping("/img/{albumID}/{pageNum}")
  public ResponseEntity<String> unloadImage(@PathVariable String albumID, @PathVariable int pageNum,
      @RequestParam String imageUUID) throws JsonProcessingException {

    List<List<ImagesInPage>> imagesInfo = albumService.unloadImage(albumID, pageNum, imageUUID);
    simpMessageTemplate.convertAndSend("/sub/edit/" + albumID, imagesInfo);

    return ResponseEntity.ok("이미지를 성공적으로 제외했습니다.");
  }

  @Operation(description = "공동 작업을 위한 웹소켓 메소드 입니다.")
  @MessageMapping("/edit/{albumID}")
  @SendTo("/sub/edit/{albumID}")
  public List<List<ImagesInPage>> editImage(@DestinationVariable String albumID, List<EditMessage> payload)
      throws Exception {

    log.info("albumID : " + albumID);

    return albumService.editImage(albumID, payload);
  }

  @Operation(description = "앨범의 프로필 변경 API 입니다.")
  @PutMapping("profile/{albumID}")
  public ResponseEntity<String> updateProfile(@PathVariable String albumID, @RequestParam String newAlbumName,
      @RequestPart(required = false) MultipartFile thumbnailImage,
      Principal principal) {

    String result = albumService.updateProfile(albumID, newAlbumName, thumbnailImage, principal.getName());

    return ResponseEntity.ok(result);
  }

  @Operation(description = "앨범 삭제 메서드입니다.")
  @DeleteMapping("/{albumID}")
  public ResponseEntity<String> removeAlbum(@PathVariable String albumID, Principal principal) throws IOException {

    String result = albumService.removeAlbum(albumID, principal.getName());

    return ResponseEntity.ok(result);
  }

  @Operation(description = "웹소켓 테스트 메소드 입니다.")
  @MessageMapping("/test/{albumID}")
  @SendTo("/sub/test/{albumID}")
  public String testWebSocket(@DestinationVariable String albumID, String payload)
      throws Exception {

    log.info("albumID : " + albumID);

    return payload;
  }

  @Operation(description = "앨범 초대 메서드입니다.")
  @PostMapping("/invitation/{albumID}/{socialID}")
  public ResponseEntity<String> sendAlbumInvitation(@PathVariable String albumID, @PathVariable String collaboUserID,
      Principal principal) {

    Album album = albumService.sendAlbumInvitation(albumID, collaboUserID, principal.getName());

    if (album == null)
      return ResponseEntity.ok("이미 친구이거나 요청을 보낸 상대입니다.");

    String result = sseEmitters.sendAlbumInvitation(collaboUserID, album);

    return ResponseEntity.ok(result);
  }

  @Operation(description = "앨범 초대 응답 엔드포인트입니다. reply 파라미터의 값이 accept 면 앨범에 참여하게 됩니다.")
  @PostMapping("/invitation-reply/{albumID}")
  public ResponseEntity<String> replyAlbumInvitation(@PathVariable String albumID, @RequestParam String reply,
      Principal principal) {

    String res = albumService.replyAlbumInvitation(albumID, principal.getName(), reply);

    return ResponseEntity.ok(res);
  }
}