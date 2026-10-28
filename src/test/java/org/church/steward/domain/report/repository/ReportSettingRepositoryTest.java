package org.church.steward.domain.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportSetting;
import org.church.steward.domain.report.entity.ReportSettingId;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.entity.ReportViewMode;
import org.church.steward.domain.report.entity.ReportZoomMode;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ReportSettingRepositoryTest {

    @Autowired
    private ReportSettingRepository reportSettingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User saveUser(String id) {
        return userRepository.save(User.builder()
                .id(id)
                .password("hashed_password")
                .role(UserRole.USER)
                .build());
    }

    private ReportSetting saveSetting(String userId, ReportType type, int lineSpacing) {
        ReportSetting setting = ReportSetting.builder()
                .userId(userId)
                .reportType(type)
                .fontFamily(ReportFontFamily.NANUM_MYEONGJO)
                .fontSizeStep(2)
                .lineSpacing(lineSpacing)
                .autoFit(true)
                .thumbnailsVisible(true)
                .viewMode(ReportViewMode.SINGLE_PAGE)
                .zoomMode(ReportZoomMode.CUSTOM)
                .zoomPercent(150)
                .build();
        return reportSettingRepository.save(setting);
    }

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("사용자와 보고서 종류가 모두 같은 설정만 찾는다")
        void findById_UserAndType_ReturnsMatchingSetting() {
            // Given
            saveUser("user1");
            saveUser("user2");
            saveSetting("user1", ReportType.WEEKLY_SETTLEMENT, 110);
            saveSetting("user1", ReportType.ANNUAL_SETTLEMENT, 120);
            saveSetting("user2", ReportType.WEEKLY_SETTLEMENT, 130);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<ReportSetting> found = reportSettingRepository
                    .findById(new ReportSettingId("user1", ReportType.ANNUAL_SETTLEMENT));

            // Then
            assertThat(found).isPresent();
            assertThat(found.get()
                    .getLineSpacing()).isEqualTo(120);
        }

        @Test
        @DisplayName("저장한 enum 값을 그대로 읽어 온다")
        void findById_Saved_ReadsEnumValues() {
            // Given
            saveUser("user1");
            saveSetting("user1", ReportType.WEEKLY_SETTLEMENT, 110);
            entityManager.flush();
            entityManager.clear();

            // When
            ReportSetting found = reportSettingRepository
                    .findById(new ReportSettingId("user1", ReportType.WEEKLY_SETTLEMENT))
                    .orElseThrow();

            // Then
            assertThat(found.getFontFamily()).isEqualTo(ReportFontFamily.NANUM_MYEONGJO);
            assertThat(found.getViewMode()).isEqualTo(ReportViewMode.SINGLE_PAGE);
            assertThat(found.getZoomMode()).isEqualTo(ReportZoomMode.CUSTOM);
        }

        @Test
        @DisplayName("사용자를 삭제하면 그 사용자의 보고서 설정도 함께 지워진다")
        void findById_UserDeleted_ReturnsEmpty() {
            // Given
            User user = saveUser("user1");
            saveSetting("user1", ReportType.WEEKLY_SETTLEMENT, 110);
            entityManager.flush();
            entityManager.clear();

            // When
            userRepository.delete(userRepository.findById(user.getId())
                    .orElseThrow());
            entityManager.flush();
            entityManager.clear();

            // Then
            assertThat(reportSettingRepository.findById(new ReportSettingId("user1", ReportType.WEEKLY_SETTLEMENT)))
                    .isEmpty();
        }
    }
}
