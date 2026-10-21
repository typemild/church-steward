package org.church.steward.domain.contact.controller;

import static org.mockito.Mockito.verify;

import org.church.steward.domain.contact.service.ContactGroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactGroupControllerTest {

    @Mock
    private ContactGroupService contactGroupService;

    @InjectMocks
    private ContactGroupController contactGroupController;

    @Nested
    @DisplayName("deleteGroup 테스트")
    class DeleteGroupTest {
        @Test
        @DisplayName("유효한 그룹 ID로 그룹 삭제 시 서비스의 deleteGroup이 호출되어야 한다")
        void deleteGroup_validGroupId_callsService() {
            // Given
            Long groupId = 1L;

            // When
            contactGroupController.deleteGroup(groupId);

            // Then
            verify(contactGroupService).deleteGroup(groupId);
        }
    }
}
