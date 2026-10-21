package org.church.steward.domain.user.controller;

import java.util.List;

import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.domain.user.service.UserService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.AdminAccessChecker;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminAccessChecker adminAccessChecker;

    public void createUser(String id, String rawPassword, String remarks) {
        adminAccessChecker.checkAdmin();
        userService.createUser(id, rawPassword, remarks);
    }

    public void updateUser(String userId, String newPassword, String remarks) {
        adminAccessChecker.checkAdmin();
        userService.updateUser(userId, newPassword, remarks);
    }

    public void deleteUser(String userId) {
        adminAccessChecker.checkAdmin();
        userService.deleteUser(userId);
    }

    public void updateUserPassword(String userId, String oldPassword, String newPassword) {
        userService.updateUserPassword(userId, oldPassword, newPassword);
    }

    public List<UserDto> listUsersByRole(UserRole role) {
        adminAccessChecker.checkAdmin();
        return userService.listUsersByRole(role);
    }
}
