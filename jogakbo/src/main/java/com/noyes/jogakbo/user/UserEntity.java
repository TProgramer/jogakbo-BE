package com.noyes.jogakbo.user;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "USER")
@Entity
public class UserEntity {

  /**
   * <pre>
   * JPA 구현체인 Hibernate의 기본키 생성 방식을 따라서 생성되는 Index 값
   * 
   * 참고)
   * 
   * `@GeneratedValue(strategy = GenerationType.xxx)`
   * : Primary Key의 키 생성 전략(Strategy)을 설정하고자 할 때 사용
   * 
   * GenerationType.IDENTITY : MySQL의 AUTO_INCREMENT 방식을 이용
   * GenerationType.AUTO(default) : JPA 구현체(Hibernate)가 생성 방식을 결정
   * GenerationType.SEQUENCE : DB의 SEQUENCE를 이용해서 키를 생성. @SequenceGenerator와 같이 사용
   * GenerationType.TABLE : 키 생성 전용 테이블을 생성해서 키 생성. @TableGenerator와 함께 사용
   * </pre>
   */
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private Long id;

  // Timestamp의 값을 현재 시간으로 유저 생성일 등록
  @Column(length = 200, nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private Timestamp regDate;

  // Social 로그인 정보 저장
  @Enumerated(EnumType.STRING)
  private SocialType socialType;
  private String socialId;

  @Enumerated(EnumType.STRING)
  private Role role;

  private int age;
  private String name;
  private String email;
  private String password;
  private String nickname;
  private String imageUrl;
  private String phoneNum;
  private String refreshToken;
  private Date birthDay;

  // 유저 권한 승격 메소드
  public void authorizeUser() {
    this.role = Role.USER;
  }

  public boolean isUserInfoEmpty() {

    return Stream.of(birthDay, phoneNum)
        .anyMatch(Objects::isNull);
  }

  // 비밀번호 암호화 메소드
  public void passwordEncode(PasswordEncoder passwordEncoder) {
    this.password = passwordEncoder.encode(this.password);
  }

  public void updateRefreshToken(String newRefreshToken) {
    this.refreshToken = newRefreshToken;
  }
}