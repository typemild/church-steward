package org.church.steward.domain.fiscalyear.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.fiscalyear.service.FiscalYearService;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.domain.user.service.UserSettingService;
import org.church.steward.infrastructure.security.LoggedInUser;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalYearControllerTest {

    @Mock
    private FiscalYearService fiscalYearService;

    @Mock
    private UserSettingService userSettingService;

    @Mock
    private UserSessionHolder userSessionHolder;

    private final LoggedInUser user = new LoggedInUser("testUser", UserRole.USER);

    @InjectMocks
    private FiscalYearController fiscalYearController;

    @Nested
    @DisplayName("listFiscalYears 테스트")
    class ListFiscalYearsTest {

        @Test
        @DisplayName("회계 연도 목록 조회 시 서비스로부터 받은 목록과 일치하는 데이터를 반환한다.")
        void listFiscalYears_whenCalled_shouldReturnMatchingList() {
            // Given
            FiscalYearDto fiscalYearDto1 = FiscalYearDto.builder().id(1L).name("2024년").build();
            FiscalYearDto fiscalYearDto2 = FiscalYearDto.builder().id(2L).name("2025년").build();
            List<FiscalYearDto> fiscalYears = List.of(fiscalYearDto1, fiscalYearDto2);
            when(fiscalYearService.listFiscalYears()).thenReturn(fiscalYears);

            // When
            List<FiscalYearDto> result = fiscalYearController.listFiscalYears();

            // Then
            assertThat(result).containsExactlyElementsOf(fiscalYears);
        }

        @Test
        @DisplayName("회계 연도 목록 조회 시 서비스의 listFiscalYears 메소드를 호출한다.")
        void listFiscalYears_whenCalled_shouldCallServiceMethod() {
            // Given
            when(fiscalYearService.listFiscalYears()).thenReturn(List.of());

            // When
            fiscalYearController.listFiscalYears();

            // Then
            verify(fiscalYearService).listFiscalYears();
        }
    }

    @Nested
    @DisplayName("createFiscalYear 테스트")
    class CreateFiscalYearTest {

        @Test
        @DisplayName("회계 연도 생성 시 서비스의 createFiscalYear 메소드를 호출한다.")
        void createFiscalYear_whenCalled_shouldCallServiceMethod() {
            // Given
            String name = "2027년";
            LocalDate startDate = LocalDate.of(2026, 12, 1);
            LocalDate endDate = LocalDate.of(2027, 11, 30);

            // When
            fiscalYearController.createFiscalYear(name, startDate, endDate);

            // Then
            verify(fiscalYearService).createFiscalYear(name, startDate, endDate);
        }
    }

    @Nested
    @DisplayName("createFiscalYearWithCopy 테스트")
    class CreateFiscalYearWithCopyTest {

        @Test
        @DisplayName("회계 연도 복사 생성 시 서비스의 createFiscalYearWithCopy 메소드를 호출한다.")
        void createFiscalYearWithCopy_whenCalled_shouldCallServiceMethod() {
            // Given
            String name = "2025년";
            Long sourceFiscalYearId = 1L;
            LocalDate startDate = LocalDate.of(2024, 12, 1);
            LocalDate endDate = LocalDate.of(2025, 11, 30);

            // When
            fiscalYearController.createFiscalYearWithCopy(name, startDate, endDate, sourceFiscalYearId);

            // Then
            verify(fiscalYearService).createFiscalYearWithCopy(name, startDate, endDate, sourceFiscalYearId);
        }
    }

    @Nested
    @DisplayName("updateFiscalYear 테스트")
    class UpdateFiscalYearTest {

        @Test
        @DisplayName("회계 연도 수정 시 서비스의 updateFiscalYear 메소드를 호출한다.")
        void updateFiscalYear_whenCalled_shouldCallServiceMethod() {
            // Given
            Long id = 2L;
            String name = "2025년";
            LocalDate startDate = LocalDate.of(2024, 12, 1);
            LocalDate endDate = LocalDate.of(2025, 11, 30);

            // When
            fiscalYearController.updateFiscalYear(id, name, startDate, endDate);

            // Then
            verify(fiscalYearService).updateFiscalYear(id, name, startDate, endDate);
        }
    }

    @Nested
    @DisplayName("deleteFiscalYear 테스트")
    class DeleteFiscalYearTest {

        @Test
        @DisplayName("회계 연도 삭제 시 서비스의 deleteFiscalYear 메소드를 호출한다.")
        void deleteFiscalYear_whenCalled_shouldCallServiceMethod() {
            // Given
            Long id = 3L;

            // When
            fiscalYearController.deleteFiscalYear(id);

            // Then
            verify(fiscalYearService).deleteFiscalYear(id);
        }
    }

    @Nested
    @DisplayName("closeFiscalYear 테스트")
    class CloseFiscalYearTest {

        @Test
        @DisplayName("회계 연도 마감 시 서비스의 closeFiscalYear 메소드를 호출한다.")
        void closeFiscalYear_whenCalled_shouldCallServiceMethod() {
            // Given
            Long id = 3L;

            // When
            fiscalYearController.closeFiscalYear(id);

            // Then
            verify(fiscalYearService).closeFiscalYear(id);
        }
    }

    @Nested
    @DisplayName("reopenFiscalYear 테스트")
    class ReopenFiscalYearTest {

        @Test
        @DisplayName("회계 연도 마감 취소 시 서비스의 reopenFiscalYear 메소드를 호출한다.")
        void reopenFiscalYear_whenCalled_shouldCallServiceMethod() {
            // Given
            Long id = 3L;

            // When
            fiscalYearController.reopenFiscalYear(id);

            // Then
            verify(fiscalYearService).reopenFiscalYear(id);
        }
    }

    @Nested
    @DisplayName("getSelectedFiscalYearId 테스트")
    class GetSelectedFiscalYearIdTest {

        @Test
        @DisplayName("선택된 회계 연도 조회 시 로그인한 사용자의 설정에 저장된 회계 연도의 ID를 반환한다.")
        void getSelectedFiscalYearId_whenCalled_shouldReturnIdOfLoggedInUser() {
            // Given
            when(userSessionHolder.getLoggedInUser()).thenReturn(user);
            when(userSettingService.getSelectedFiscalYearId("testUser")).thenReturn(4L);

            // When
            Long result = fiscalYearController.getSelectedFiscalYearId();

            // Then
            assertThat(result).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("updateSelectedFiscalYearId 테스트")
    class UpdateSelectedFiscalYearIdTest {

        @Test
        @DisplayName("선택된 회계 연도 저장 시 로그인한 사용자의 ID로 서비스의 updateSelectedFiscalYearId 메소드를 호출한다.")
        void updateSelectedFiscalYearId_whenCalled_shouldCallServiceMethodWithLoggedInUserId() {
            // Given
            when(userSessionHolder.getLoggedInUser()).thenReturn(user);

            // When
            fiscalYearController.updateSelectedFiscalYearId(5L);

            // Then
            verify(userSettingService).updateSelectedFiscalYearId("testUser", 5L);
        }
    }
}
