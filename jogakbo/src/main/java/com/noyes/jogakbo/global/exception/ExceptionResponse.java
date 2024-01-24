package com.noyes.jogakbo.global.exception;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionResponse {
  private Date timestamp; // 에러가 발생한 시간
  private String message; // 메세지 담을 필드
  private String details; // 상세내용 담을 필드
}
