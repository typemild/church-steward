package org.church.steward.domain.contact.repository;

import java.util.Collection;
import java.util.List;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * 자동 완성 후보를 이름 일치 → 이름이 검색어로 시작 → 이름에 검색어 포함 → 비고에 검색어 포함 순으로 조회한다. 같은 순위 안에서는
     * 이름순이다. 후보는 숨기지 않았고 제외 목록에 없으며, 이름이나 비고에 검색어가 들어 있는 관계자다. 검색어를 LIKE 패턴이 아닌 글자
     * 그대로 찾도록 LOCATE를 쓴다. 비고가 null이면 LOCATE도 null이 되어 조건에서 빠진다. 조건은
     * {@link #countAutocompleteCandidates}와 같아야 한다.
     */
    @Query("""
            SELECT c
              FROM Contact c
             WHERE c.isHidden = false
               AND c.id NOT IN :excludedIds
               AND (LOCATE(:keyword, c.name) > 0 OR LOCATE(:keyword, c.remarks) > 0)
             ORDER BY CASE WHEN c.name = :keyword THEN 0
                           WHEN LOCATE(:keyword, c.name) = 1 THEN 1
                           WHEN LOCATE(:keyword, c.name) > 0 THEN 2
                           ELSE 3
                      END
                    , c.name ASC
                    , c.id ASC
            """)
    List<Contact> findAutocompleteCandidates(@Param("keyword") String keyword,
            @Param("excludedIds") Collection<Long> excludedIds,
            Pageable pageable);

    /**
     * 자동 완성 후보의 수를 센다. 조건은 {@link #findAutocompleteCandidates}와 같아야 한다.
     */
    @Query("""
            SELECT COUNT(c)
              FROM Contact c
             WHERE c.isHidden = false
               AND c.id NOT IN :excludedIds
               AND (LOCATE(:keyword, c.name) > 0 OR LOCATE(:keyword, c.remarks) > 0)
            """)
    long countAutocompleteCandidates(@Param("keyword") String keyword,
            @Param("excludedIds") Collection<Long> excludedIds);

    /**
     * 관계자 관리 목록을 조건으로 조회한다. 조건이 null이면 적용하지 않는다. 이름과 비고는 검색어를 LIKE 패턴이 아닌 글자 그대로
     * 찾도록 LOCATE를 쓴다.
     */
    @Query("""
            SELECT c
              FROM Contact c
             WHERE (:type IS NULL OR c.type = :type)
               AND (:name IS NULL OR LOCATE(:name, c.name) > 0)
               AND (:remarks IS NULL OR LOCATE(:remarks, c.remarks) > 0)
               AND (:includeHidden = true OR c.isHidden = false)
             ORDER BY c.name ASC
            """)
    List<Contact> searchContacts(@Param("type") ContactType type,
            @Param("name") String name,
            @Param("remarks") String remarks,
            @Param("includeHidden") Boolean includeHidden);
}
