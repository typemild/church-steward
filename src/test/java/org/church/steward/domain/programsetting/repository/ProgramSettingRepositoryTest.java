package org.church.steward.domain.programsetting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;

import org.church.steward.domain.programsetting.entity.ProgramSetting;
import org.church.steward.domain.programsetting.entity.ProgramSettingConstraints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ProgramSettingRepositoryTest {

    @Autowired
    private ProgramSettingRepository programSettingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("마이그레이션 직후에는 설정 행이 없다")
        void findById_AfterMigration_ReturnsEmpty() {
            // When & Then
            assertThat(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)).isEmpty();
        }

        @Test
        @DisplayName("저장한 결산 요일을 그대로 읽어 온다")
        void findById_Saved_ReadsWeeklyClosingDay() {
            // Given
            programSettingRepository.save(ProgramSetting.builder()
                    .id(ProgramSettingConstraints.SETTING_ID)
                    .weeklyClosingDay(DayOfWeek.SATURDAY)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // When
            ProgramSetting found = programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)
                    .orElseThrow();

            // Then
            assertThat(found.getWeeklyClosingDay()).isEqualTo(DayOfWeek.SATURDAY);
        }
    }
}
