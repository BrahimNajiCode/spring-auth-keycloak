package com.brahim.auth.service;

import com.brahim.auth.dto.response.UserResponse;

public interface UserService {
    UserResponse getCurrentUser();

    UserResponse getUserByKeycloakId(String keycloakId);

    void assignRole(String keycloakId, String roleName);

    void removeRole(String keycloakId, String roleName);
}
