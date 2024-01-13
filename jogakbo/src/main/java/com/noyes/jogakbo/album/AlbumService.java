package com.noyes.jogakbo.album;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noyes.jogakbo.album.DTO.EditMessage;
import com.noyes.jogakbo.album.DTO.EntryMessage;
import com.noyes.jogakbo.album.DTO.ImageInfo;
import com.noyes.jogakbo.album.DTO.ImagesInPage;
import com.noyes.jogakbo.global.redis.RedisService;
import com.noyes.jogakbo.global.s3.AwsS3Service;
import com.noyes.jogakbo.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

  private final UserService userService;
  private final AwsS3Service awsS3Service;
  private final RedisService redisService;

  public EntryMessage getEntryMessage(String socialID, String albumID) throws JsonProcessingException {

    String albumName = userService.getAlbumByUser(socialID, albumID).getAlbumName();
    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();

    return EntryMessage.builder()
        .albumName(albumName)
        .imagesInfo(imagesInfo)
        .build();
  }

  public List<Album> getAllAlbumByUser(String socialID) {

    return userService.getAlbumsByUser(socialID);
  }

  public String createAlbum(String albumName, String socialID) throws JsonProcessingException {

    String albumID = UUID.randomUUID().toString();
    List<List<ImagesInPage>> blankImagesProp = new ArrayList<>();
    blankImagesProp.add(new ArrayList<>());

    Album newAlbum = Album.builder()
        .albumID(albumID)
        .albumName(albumName)
        .images(blankImagesProp)
        .albumEditors(new ArrayList<>())
        .build();

    userService.addAlbum(newAlbum, socialID);

    AlbumImagesInfo albumImagesInfo = AlbumImagesInfo.builder()
        .id(albumID)
        .imagesInfo(blankImagesProp)
        .build();

    redisService.setAlbumRedisValue(albumID, albumImagesInfo);

    return albumID;
  }

  public List<List<ImagesInPage>> addNewPage(String albumID) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();
    imagesInfo.add(new ArrayList<>());
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }

  public List<List<ImagesInPage>> uploadImages(String albumID, List<MultipartFile> multipartFiles, String fileInfos)
      throws JsonProcessingException {

    // S3에 업로드 시도 후, 업로드 된 S3 파일명 리스트로 받아오기
    List<String> uploadFileNames = awsS3Service.uploadFile(multipartFiles);

    ObjectMapper objectMapper = new ObjectMapper();
    List<ImageInfo> imageInfos = objectMapper.readValue(fileInfos, new TypeReference<List<ImageInfo>>() {
    });

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();

    for (int i = 0; i < uploadFileNames.size(); i++) {

      int pageNum = imageInfos.get(i).getPage();
      List<ImagesInPage> targetPageInfo = imagesInfo.get(pageNum);
      ImagesInPage tmp = ImagesInPage.builder()
          .imageUUID(uploadFileNames.get(i))
          .size(imageInfos.get(i).getSize())
          .location(imageInfos.get(i).getLocation())
          .rotation(imageInfos.get(i).getRotation())
          .build();

      targetPageInfo.add(tmp);
      imagesInfo.set(pageNum, targetPageInfo);
    }

    log.info(imagesInfo.toString());
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }

  public void unloadImage(String albumID, int pageNum, String imageUUID) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<ImagesInPage> targetList = targetInfo.getImagesInfo().get(pageNum);

    for (ImagesInPage tmp : targetList) {

      if (tmp.getImageUUID().equals(imageUUID)) {

        targetList.remove(tmp);
        break;
      }
    }
    redisService.setAlbumRedisValue(albumID, targetInfo);

    awsS3Service.deleteFile(imageUUID);
  }

  public List<List<ImagesInPage>> editImage(String albumID, List<EditMessage> payload) throws JsonProcessingException {

    AlbumImagesInfo targetInfo = redisService.getAlbumRedisValue(albumID, AlbumImagesInfo.class);
    List<List<ImagesInPage>> imagesInfo = targetInfo.getImagesInfo();

    for (EditMessage target : payload) {

      int pageNum = target.getImageInfo().getPage();
      List<ImagesInPage> targetPageInfo = imagesInfo.get(pageNum);

      // To-Do: imageUUID가 같은지 확인하는 효율적인 로직 찾아보기
      for (ImagesInPage tmp : targetPageInfo) {

        if (tmp.getImageUUID().equals(target.getImageUUID())) {

          tmp.setLocation(target.getImageInfo().getLocation());
          tmp.setSize(target.getImageInfo().getSize());
          tmp.setRotation(target.getImageInfo().getRotation());

          targetPageInfo.add(tmp);
          targetPageInfo.remove(tmp);
          log.info("변경사항 적용 완료");
          break;
        }
      }
    }

    // albumImagesInfoRepository.save(targetInfo);
    redisService.setAlbumRedisValue(albumID, targetInfo);

    return imagesInfo;
  }
}