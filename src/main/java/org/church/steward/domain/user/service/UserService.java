package org.church.steward.domain.user.service;

import java.util.List;

import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.UserNotFoundException;

/**
 * 사용자 관리를 담당하는 서비스 인터페이스다.
 */
public interface UserService {

    /**
     * 새로운 사용자를 추가한다.
     *
     * @param id          사용자 ID (성명)
     * @param rawPassword 암호화 전 비밀번호
     * @param remarks     비고
     * @throws IllegalArgumentException 이미 존재하는 사용자 ID인 경우 발생
     */
    void createUser(String id, String rawPassword, String remarks);

    /**
     * 새로운 관리자를 추가한다.
     *
     * @param id          사용자 ID (성명)
     * @param rawPassword 암호화 전 비밀번호
     * @param remarks     비고
     * @throws IllegalArgumentException 이미 존재하는 사용자 ID인 경우 발생
     */
    void createAdmin(String id, String rawPassword, String remarks);

    /**
     * 사용자를 인증하고 로그인 세션을 생성한다.
     *
     * @param id          사용자 ID (성명)
     * @param rawPassword 암호화 전 비밀번호
     * @return 로그인에 성공한 User 엔티티
     * @throws UserNotFoundException    사용자 정보가 존재하지 않을 때 발생
     * @throws InvalidPasswordException 사용자 비밀번호가 잘못되었을 때 발생
     */
    User login(String id, String rawPassword);

    /**
     * 사용자의 비밀번호를 변경한다.
     *
     * @param userId      사용자 ID (성명)
     * @param oldPassword 현재 비밀번호 (암호화 전)
     * @param newPassword 새로운 비밀번호 (암호화 전)
     * @throws IllegalArgumentException 현재 비밀번호가 일치하지 않거나 사용자를 찾을 수 없는 경우 발생
     */
    void updateUserPassword(String userId, String oldPassword, String newPassword);

    void updateUser(String userId, String newPassword, String remarks);

    void deleteUser(String userId);

    List<UserDto> listUsersByRole(UserRole role);
}
