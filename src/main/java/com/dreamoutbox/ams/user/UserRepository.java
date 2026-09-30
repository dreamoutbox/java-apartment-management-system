package com.dreamoutbox.ams.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(Role role);
    List<User> findByRoleOrderByCreatedAtAsc(Role role);
    List<User> findByRoleAndDeletedFalseOrderByCreatedAtAsc(Role role);
    List<User> findAllByOrderByCreatedAtAsc();
    List<User> findAllByDeletedFalseOrderByCreatedAtAsc();
    boolean existsByUsername(String username);
}
