package org.church.steward.domain.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.church.steward.domain.fiscalyear.service.FiscalYearService;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserSetting;
import org.church.steward.domain.user.repository.UserRepository;
import org.church.steward.domain.user.repository.UserSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceImplTest {

    private static final String USER_ID = "user1";

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FiscalYearService fiscalYearService;

    @InjectMocks
    private UserSettingServiceImpl userSettingService;

    private UserSetting createUserSetting(Long selectedFiscalYearId) {
        return UserSetting.builder()
                .userId(USER_ID)
                .selectedFiscalYearId(selectedFiscalYearId)
                .build();
    }

    @Nested
    @DisplayName("getSelectedFiscalYearId 테스트")
    class GetSelectedFiscalYearIdTest {

        @Test
        @DisplayName("설정된 회계 연도가 존재하면 그 회계 연도의 ID를 반환한다")
        void getSelectedFiscalYearId_ExistingFiscalYear_ReturnsSetting() {
            // Given
            Long selectedId = 3L;
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.of(createUserSetting(selectedId)));
            when(fiscalYearService.existsById(selectedId)).thenReturn(true);

            // When
            Long actualId = userSettingService.getSelectedFiscalYearId(USER_ID);

            // Then
            assertThat(actualId).isEqualTo(selectedId);
        }

        @Test
        @DisplayName("설정된 회계 연도가 존재하면 설정을 다시 저장하지 않는다")
        void getSelectedFiscalYearId_ExistingFiscalYear_DoesNotSave() {
            // Given
            Long selectedId = 3L;
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.of(createUserSetting(selectedId)));
            when(fiscalYearService.existsById(selectedId)).thenReturn(true);

            // When
            userSettingService.getSelectedFiscalYearId(USER_ID);

            // Then
            verify(userSettingRepository, never()).save(any(UserSetting.class));
        }

        @Test
        @DisplayName("설정된 회계 연도가 삭제된 상태라면 가장 최근 회계 연도의 ID를 반환한다")
        void getSelectedFiscalYearId_DeletedFiscalYear_ReturnsLatest() {
            // Given
            Long deletedId = 2L;
            Long latestId = 5L;
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.of(createUserSetting(deletedId)));
            when(fiscalYearService.existsById(deletedId)).thenReturn(false);
            when(fiscalYearService.getLatestFiscalYearId()).thenReturn(latestId);

            // When
            Long actualId = userSettingService.getSelectedFiscalYearId(USER_ID);

            // Then
            assertThat(actualId).isEqualTo(latestId);
        }

        @Test
        @DisplayName("설정된 회계 연도가 삭제된 상태라면 가장 최근 회계 연도를 설정에 저장한다")
        void getSelectedFiscalYearId_DeletedFiscalYear_SavesLatest() {
            // Given
            Long deletedId = 2L;
            Long latestId = 5L;
            UserSetting userSetting = createUserSetting(deletedId);
            ArgumentCaptor<UserSetting> userSettingCaptor = ArgumentCaptor.forClass(UserSetting.class);
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.of(userSetting));
            when(fiscalYearService.existsById(deletedId)).thenReturn(false);
            when(fiscalYearService.getLatestFiscalYearId()).thenReturn(latestId);

            // When
            userSettingService.getSelectedFiscalYearId(USER_ID);

            // Then
            verify(userSettingRepository).save(userSettingCaptor.capture());
            assertThat(userSettingCaptor.getValue().getSelectedFiscalYearId()).isEqualTo(latestId);
        }

        @Test
        @DisplayName("사용자 설정이 없으면 가장 최근 회계 연도의 ID를 반환한다")
        void getSelectedFiscalYearId_NoSetting_ReturnsLatest() {
            // Given
            Long latestId = 5L;
            User user = User.builder().id(USER_ID).build();
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.empty());
            when(fiscalYearService.existsById(null)).thenReturn(false);
            when(fiscalYearService.getLatestFiscalYearId()).thenReturn(latestId);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            Long actualId = userSettingService.getSelectedFiscalYearId(USER_ID);

            // Then
            assertThat(actualId).isEqualTo(latestId);
        }

        @Test
        @DisplayName("사용자 설정이 없으면 가장 최근 회계 연도로 설정을 새로 저장한다")
        void getSelectedFiscalYearId_NoSetting_SavesLatest() {
            // Given
            Long latestId = 5L;
            User user = User.builder().id(USER_ID).build();
            ArgumentCaptor<UserSetting> userSettingCaptor = ArgumentCaptor.forClass(UserSetting.class);
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.empty());
            when(fiscalYearService.existsById(null)).thenReturn(false);
            when(fiscalYearService.getLatestFiscalYearId()).thenReturn(latestId);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            userSettingService.getSelectedFiscalYearId(USER_ID);

            // Then
            verify(userSettingRepository).save(userSettingCaptor.capture());
            assertThat(userSettingCaptor.getValue().getSelectedFiscalYearId()).isEqualTo(latestId);
        }
    }

    @Nested
    @DisplayName("updateSelectedFiscalYearId 테스트")
    class UpdateSelectedFiscalYearIdTest {

        @Test
        @DisplayName("기존 설정이 있으면 해당 설정의 회계 연도를 바꿔 저장한다")
        void updateSelectedFiscalYearId_ExistingSetting_Updates() {
            // Given
            Long newId = 5L;
            UserSetting userSetting = createUserSetting(3L);
            ArgumentCaptor<UserSetting> userSettingCaptor = ArgumentCaptor.forClass(UserSetting.class);
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.of(userSetting));

            // When
            userSettingService.updateSelectedFiscalYearId(USER_ID, newId);

            // Then
            verify(userSettingRepository).save(userSettingCaptor.capture());
            assertThat(userSettingCaptor.getValue()).isSameAs(userSetting);
            assertThat(userSetting.getSelectedFiscalYearId()).isEqualTo(newId);
        }

        @Test
        @DisplayName("설정이 없으면 새로운 설정을 생성하여 저장한다")
        void updateSelectedFiscalYearId_NoSetting_CreatesAndSaves() {
            // Given
            User user = User.builder().id(USER_ID).build();
            when(userSettingRepository.findById(USER_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            userSettingService.updateSelectedFiscalYearId(USER_ID, 5L);

            // Then
            verify(userSettingRepository).save(any(UserSetting.class));
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외가 발생한다")
        void updateSelectedFiscalYearId_UserNotFound_ThrowsException() {
            // Given
            String userId = "unknown";
            when(userSettingRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userSettingService.updateSelectedFiscalYearId(userId, 5L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }
}
