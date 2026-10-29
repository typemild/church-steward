package org.church.steward.domain.user.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserConstraints;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.domain.user.mapper.UserMapper;
import org.church.steward.domain.user.repository.UserRepository;
import org.church.steward.domain.user.service.UserService;
import org.church.steward.infrastructure.config.DatabaseManager;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.LoggedInUser;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * UserService 인터페이스의 구현체다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionHolder userSessionHolder;
    private final DatabaseManager databaseManager;
    private final UserMapper userMapper;
    private final ActivityLogService activityLogService;
    private final EntitySnapshotCapturer entitySnapshotCapturer;

    @Override
    public void createUser(String id, String rawPassword, String remarks) {
        // 1. ID 패턴과 비고 길이 검증
        validateId(id);
        validateRemarks(remarks);

        // 2. 중복 사용자 확인
        if (userRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다: " + id);
        }

        // 2. 비밀번호 해싱 (Argon2id)
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 3. User 엔티티 생성 및 저장
        User user = User.builder()
                .id(id)
                .password(encodedPassword)
                .role(UserRole.USER)
                .remarks(remarks)
                .build();

        // 4. DatabaseManager의 설정 파일 내 DB 접근 키 재암호화 및 저장
        databaseManager.saveOrUpdateUserPassword(id, rawPassword);

        userRepository.save(user);
        activityLogService.logCreation(user);
    }

    @Override
    public void createAdmin(String id, String rawPassword, String remarks) {
        // 1. ID 패턴과 비고 길이 검증
        validateId(id);
        validateRemarks(remarks);

        // 2. 중복 사용자 확인
        if (userRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다: " + id);
        }

        // 2. 비밀번호 해싱 (Argon2id)
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 3. User 엔티티 생성 및 저장
        User user = User.builder()
                .id(id)
                .password(encodedPassword)
                .role(UserRole.ADMIN)
                .remarks(remarks)
                .build();

        userRepository.save(user);
        activityLogService.logCreation(user);
    }

    @Override
    public User login(String id, String rawPassword) {
        // 1. 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + id));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 세션에 사용자 정보 저장
        userSessionHolder.setLoggedInUser(new LoggedInUser(user.getId(), user.getRole()));

        // 4. 로그인 로그 기록
        activityLogService.logLogin(user.getId());

        return user;
    }

    @Override
    public void updateUserPassword(String id, String oldPassword, String newPassword) {
        // 1. 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호는 위에서 확인했으므로 입력값끼리 비교한다.
        if (newPassword.equals(oldPassword)) {
            throw new IllegalArgumentException("새 비밀번호가 현재 비밀번호와 같습니다.");
        }

        // 3. 변경 전 스냅샷 캡처
        EntitySnapshot<User> oldSnapshot = entitySnapshotCapturer.capture(user);

        // 4. 새로운 비밀번호 해싱 및 저장
        String encodedPassword = passwordEncoder.encode(newPassword);

        user.setPassword(encodedPassword);
        userRepository.save(user);
        activityLogService.logUpdate(oldSnapshot, user);

        // 5. DatabaseManager의 설정 파일 내 DB 접근 키 재암호화 및 저장
        databaseManager.saveOrUpdateUserPassword(id, newPassword);
    }

    @Override
    public void updateUser(String id, String newPassword, String remarks) {
        validateRemarks(remarks);

        // 1. 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        // 2. 변경 전 스냅샷 캡처
        EntitySnapshot<User> oldSnapshot = entitySnapshotCapturer.capture(user);

        // 3. 비밀번호 변경 (제공된 경우에만)
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));

            // 4. DatabaseManager의 설정 파일 내 DB 접근 키 재암호화 및 저장
            databaseManager.saveOrUpdateUserPassword(id, newPassword);
        }

        // 5. 비고 수정
        user.setRemarks(remarks);
        userRepository.save(user);
        activityLogService.logUpdate(oldSnapshot, user);
    }

    @Override
    public void deleteUser(String id) {
        // 1. 사용자 존재 확인 및 삭제
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        // 2. DatabaseManager의 설정 파일 내 DB 접근 키 삭제
        databaseManager.removeUserPassword(id);

        userRepository.delete(user);
        activityLogService.logDeletion(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> listUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validateId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수 입력 사항입니다.");
        }
        if (id.length() > UserConstraints.ID_MAX_LENGTH) {
            throw new IllegalArgumentException("사용자 ID는 " + UserConstraints.ID_MAX_LENGTH + "자 이내로 입력해주세요.");
        }

        // 패턴: 첫 글자는 한글 또는 영문, 이후는 한글, 영문, 숫자, 언더바(_)
        String pattern = "^[a-zA-Zㄱ-ㅎ가-힣][a-zA-Z0-9ㄱ-ㅎ가-힣_]*$";
        if (!id.matches(pattern)) {
            throw new IllegalArgumentException("사용자 ID는 한글, 영문, 숫자로 구성되어야 하며, " +
                    "첫 글자는 한글 또는 영문이어야 합니다. (언더바(_) 사용 가능)");
        }
    }

    private void validateRemarks(String remarks) {
        if (remarks != null && remarks.length() > UserConstraints.REMARKS_MAX_LENGTH) {
            throw new IllegalArgumentException("비고는 " + UserConstraints.REMARKS_MAX_LENGTH + "자 이내로 입력해주세요.");
        }
    }
}
