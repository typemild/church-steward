package org.church.steward.domain.report.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link ReportSetting}의 키. 사용자와 보고서 종류의 조합이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportSettingId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private ReportType reportType;
}
