
package com.cyberrange.backend.user.repository;

import com.cyberrange.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used by Login
    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    // Used to check existing email
    Optional<User> findByEmail(String email);

    // Used to check duplicate username
    boolean existsByUsername(String username);

    // Used to check duplicate email
    boolean existsByEmail(String email);

    // Used to get user details by ID
    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);

    // Search, filter and paginate users
    @EntityGraph(attributePaths = "role")
    @Query("""
            SELECT u
            FROM User u
            JOIN u.role r
            WHERE
                (
                    :keyword = ''
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                AND (:role IS NULL OR r.name = :role)
                AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("status") String status,
            Pageable pageable
    );
}
