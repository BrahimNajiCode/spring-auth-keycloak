package com.brahim.auth.model;

import com.brahim.auth.dto.request.RegisterRequest;
import com.brahim.auth.dto.response.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserMapper {

    public User toUser(RegisterRequest request, String keycloakId){
        return User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .username(request.username())
                .keycloakId(keycloakId)
                .build();
    }
    public UserResponse toUserResponse(User user){
        return new UserResponse(
                user.getId(),
                user.getKeycloakId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                List.of("ROLE_USER"),
                false,
                user.getCreatedAt()
        );
    }
}
