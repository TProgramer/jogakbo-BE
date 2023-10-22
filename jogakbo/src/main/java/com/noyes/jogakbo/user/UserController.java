package com.noyes.jogakbo.user;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

  private final UserRepository userRepository;

  @Secured("ROLE_USER")
  @QueryMapping
  public UserEntity getUser(@Argument Long id) {
    return userRepository.findById(id).get();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @QueryMapping
  public List<UserEntity> getUsers() {
    return userRepository.findAll();
  }

  @MutationMapping
  public UserEntity save(@Argument String name, @Argument int age) {
    UserEntity user = UserEntity.builder()
        .name(name)
        .age(age)
        .build();
    return userRepository.save(user);
  }

}
