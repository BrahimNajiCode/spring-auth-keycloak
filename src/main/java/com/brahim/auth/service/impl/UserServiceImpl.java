package com.brahim.auth.service.impl;

import com.brahim.auth.config.KeycloakProperties;
import com.brahim.auth.dto.response.UserResponse;
import com.brahim.auth.exception.BusinessException;
import com.brahim.auth.model.User;
import com.brahim.auth.repository.UserRepository;
import com.brahim.auth.security.SecurityContextHelper;
import com.brahim.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.brahim.auth.exception.ErrorCode.USER_NOT_FOUND;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final Keycloak keycloak;
    private final KeycloakProperties props;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    @Override
    public UserResponse getCurrentUser() {
        String keycloakId = securityContextHelper.getCurrentUserId();
        return getUserByKeycloakId(keycloakId);
    }

    @Override
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND ,keycloakId));

        // Fetch roles from Keycloak (source of truth for roles)
        List<String> roles = getRealmRoles(keycloakId);

        return new UserResponse(
                user.getId(),
                user.getKeycloakId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                false,
                user.getCreatedAt()
        );
    }

    @Override
    public void assignRole(String keycloakId, String roleName) {
        log.info("Assigning role '{}' to user '{}'", roleName, keycloakId);

        RoleRepresentation role = keycloak.realm(props.realm())
                .roles()
                .get(roleName)
                .toRepresentation();

        getUserResource(keycloakId).roles().realmLevel().add(List.of(role));
    }

    @Override
    public void removeRole(String keycloakId, String roleName) {
        log.info("Removing role '{}' from user '{}'", roleName, keycloakId);

        RoleRepresentation role = keycloak.realm(props.realm())
                .roles()
                .get(roleName)
                .toRepresentation();

        getUserResource(keycloakId).roles().realmLevel().remove(List.of(role));
    }

    private List<String> getRealmRoles(String keycloakId) {
        return getUserResource(keycloakId).roles().realmLevel().listEffective()
                .stream()
                .map(RoleRepresentation::getName)
                .filter(r -> !r.startsWith("default-roles"))   // filter Keycloak internals
                .toList();
    }

    private UserResource getUserResource(String keycloakId) {
        return keycloak.realm(props.realm()).users().get(keycloakId);
    }
}
