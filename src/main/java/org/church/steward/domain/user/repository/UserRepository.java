package org.church.steward.domain.user.repository;

import java.util.List;

import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    List<User> findByRole(UserRole role);
}
