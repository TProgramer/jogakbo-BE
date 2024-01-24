package com.noyes.jogakbo.global.exception;

public class InvalidLoginTokenException extends RuntimeException {

  public InvalidLoginTokenException(String errorMessage) {

    super(errorMessage);
  }
}
