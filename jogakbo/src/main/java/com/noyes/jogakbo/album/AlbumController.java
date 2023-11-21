package com.noyes.jogakbo.album;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AlbumController {
  private final AlbumService albumService;

  @RequestMapping("/album/list")
  public List<Album> albumList() {
    List<Album> albumList = albumService.findAllAlbum();
    return albumList;
  }

  @PostMapping("/album")
  public void createAlbum(@RequestParam String name, Principal principal) {
    albumService.createAlbum(name);
  }

  @GetMapping("/album")
  public Album album(@RequestParam String albumId) {
    Album album = albumService.findAlbumById(albumId);
    return album;
  }
}