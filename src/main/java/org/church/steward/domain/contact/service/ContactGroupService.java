package org.church.steward.domain.contact.service;

import java.util.List;

public interface ContactGroupService {
    /**
     * 주어진 관계자들과 구성원이 정확히 일치하는 그룹이 없으면 새 그룹을 만든다.
     * 구성원의 순서는 비교하지 않으며, 중복된 관계자 ID는 한 명으로 취급한다.
     *
     * @param contactIds 그룹 구성원이 될 관계자 ID 목록
     */
    void createGroupIfAbsent(List<Long> contactIds);

    void deleteGroup(Long groupId);
}
