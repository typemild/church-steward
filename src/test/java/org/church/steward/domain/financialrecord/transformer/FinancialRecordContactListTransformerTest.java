package org.church.steward.domain.financialrecord.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialRecordContactListTransformerTest {

    private final FinancialRecordContactListTransformer transformer = new FinancialRecordContactListTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("유효한 FinancialRecordContact 리스트가 전달되면 이름들이 쉼표로 구분된 문자열을 반환한다")
        void transform_withValidList_returnsCommaSeparatedNames() {
            // Given
            FinancialRecord financialRecord = mock(FinancialRecord.class);
            Contact contact1 = mock(Contact.class);
            Contact contact2 = mock(Contact.class);

            FinancialRecordContact recordContact1 = FinancialRecordContact.builder()
                    .financialRecord(financialRecord)
                    .contact(contact1)
                    .contactName("홍길동")
                    .build();

            FinancialRecordContact recordContact2 = FinancialRecordContact.builder()
                    .financialRecord(financialRecord)
                    .contact(contact2)
                    .contactName("이순신")
                    .build();

            List<FinancialRecordContact> list = Arrays.asList(recordContact1, recordContact2);

            // When
            Object result = transformer.transform(list);

            // Then
            assertThat(result).isEqualTo("홍길동, 이순신");
        }

        @Test
        @DisplayName("빈 리스트가 전달되면 빈 문자열을 반환한다")
        void transform_withEmptyList_returnsEmptyString() {
            // Given
            List<FinancialRecordContact> list = Collections.emptyList();

            // When
            Object result = transformer.transform(list);

            // Then
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("null이 전달되면 IllegalArgumentException을 발생시킨다")
        void transform_withNull_throwsIllegalArgumentException() {
            // Given
            Object input = null;

            // When
            // Then
            assertThatThrownBy(() -> transformer.transform(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("리스트가 아닌 값이 전달되면 IllegalArgumentException을 발생시킨다")
        void transform_withNonListValue_throwsIllegalArgumentException() {
            // Given
            String notAList = "not a list";

            // When
            // Then
            assertThatThrownBy(() -> transformer.transform(notAList))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("리스트에 FinancialRecordContact가 아닌 객체가 포함되어 있으면 IllegalArgumentException을 발생시킨다")
        void transform_withInvalidItemInList_throwsIllegalArgumentException() {
            // Given
            List<Object> list = Arrays.asList(mock(FinancialRecordContact.class), "not a contact");

            // When
            // Then
            assertThatThrownBy(() -> transformer.transform(list))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
