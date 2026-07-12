package com.brahim.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_keycloak_id", columnNames = "keycloak_id"),
            @UniqueConstraint(name = "uk_users_email",       columnNames = "email"),
            @UniqueConstraint(name = "uk_users_username",    columnNames = "username")
    }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;


    /** The Keycloak user UUID — immutable after creation. */
    @Column(name = "keycloak_id", nullable = false, updatable = false)
    private String keycloakId;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    /** Audit Columns
     * You can create a BaseEntity class that gather all the audit columns and inherit it
     * so you can use it multiple times
     * */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Business key equals/hashCode (NOT surrogate PK)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return keycloakId != null && keycloakId.equals(other.keycloakId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();   // stable across transient/persistent states
    }

}
