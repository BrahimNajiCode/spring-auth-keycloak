package com.brahim.auth.repository;

import com.brahim.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User.
 *
 * SENIOR TIP:
 *   Prefer derived query methods (findByEmail) for simple lookups.
 *   Use @Query (JPQL, not SQL) only when the derived method would become
 *   unreadable (findByFirstNameContainingIgnoreCaseAndLastNameContaining...).
 *   Avoid native SQL queries unless performance profiling demands it —
 *   they bypass Hibernate's type safety and portability.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    /** Example of a JPQL query for when derived method names get unwieldy. */
    @Query("SELECT u FROM User u WHERE u.email = :email OR u.username = :username")
    Optional<User> findByEmailOrUsername(String email, String username);
}
