package org.church.steward.domain.report.entity;

import org.church.steward.domain.user.entity.UserConstraints;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 보고서마다 정한 서식과 미리보기 설정. 사용자를 삭제하면 스키마의 외래 키(ON DELETE CASCADE)가 함께 지운다.
 */
@Entity
@Table(name = "REPORT_SETTING")
@IdClass(ReportSettingId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportSetting {

    @Id
    @Column(name = "user_id", length = UserConstraints.ID_MAX_LENGTH)
    private String userId;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "report_type", length = ReportSettingConstraints.REPORT_TYPE_MAX_LENGTH)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = ReportSettingConstraints.ENUM_MAX_LENGTH)
    private ReportFontFamily fontFamily;

    /**
     * 글자 크기 조정 단계. 0이 기본 크기이고 한 단계가
     * {@link ReportSettingConstraints#FONT_SIZE_STEP_POINTS}pt다.
     */
    @Column(nullable = false)
    private int fontSizeStep;

    /** 행 간격(%) */
    @Column(nullable = false)
    private int lineSpacing;

    /** 한 페이지에 담기도록 글자 크기와 행 간격을 자동으로 줄이는지 여부 */
    @Column(nullable = false)
    private boolean autoFit;

    /** 미리보기 왼쪽의 페이지 목록을 보여주는지 여부 */
    @Column(nullable = false)
    private boolean thumbnailsVisible;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = ReportSettingConstraints.ENUM_MAX_LENGTH)
    private ReportViewMode viewMode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = ReportSettingConstraints.ENUM_MAX_LENGTH)
    private ReportZoomMode zoomMode;

    /** 직접 지정한 배율(%). 폭 맞춤과 페이지 맞춤에서는 쓰지 않지만 마지막으로 지정한 값을 남겨 둔다. */
    @Column(nullable = false)
    private int zoomPercent;

    @Builder
    public ReportSetting(String userId, ReportType reportType, ReportFontFamily fontFamily, int fontSizeStep,
            int lineSpacing, boolean autoFit, boolean thumbnailsVisible, ReportViewMode viewMode,
            ReportZoomMode zoomMode, int zoomPercent) {
        this.userId = userId;
        this.reportType = reportType;
        this.fontFamily = fontFamily;
        this.fontSizeStep = fontSizeStep;
        this.lineSpacing = lineSpacing;
        this.autoFit = autoFit;
        this.thumbnailsVisible = thumbnailsVisible;
        this.viewMode = viewMode;
        this.zoomMode = zoomMode;
        this.zoomPercent = zoomPercent;
    }

    /**
     * 설정하지 않은 사용자에게 적용하는 기본 설정
     */
    public static ReportSetting defaults(String userId, ReportType reportType) {
        return ReportSetting.builder()
                .userId(userId)
                .reportType(reportType)
                .fontFamily(ReportSettingConstraints.DEFAULT_FONT_FAMILY)
                .fontSizeStep(ReportSettingConstraints.DEFAULT_FONT_SIZE_STEP)
                .lineSpacing(ReportSettingConstraints.DEFAULT_LINE_SPACING)
                .autoFit(ReportSettingConstraints.DEFAULT_AUTO_FIT)
                .thumbnailsVisible(ReportSettingConstraints.DEFAULT_THUMBNAILS_VISIBLE)
                .viewMode(ReportSettingConstraints.DEFAULT_VIEW_MODE)
                .zoomMode(ReportSettingConstraints.DEFAULT_ZOOM_MODE)
                .zoomPercent(ReportSettingConstraints.DEFAULT_ZOOM)
                .build();
    }

    public void update(ReportFontFamily fontFamily, int fontSizeStep, int lineSpacing, boolean autoFit,
            boolean thumbnailsVisible, ReportViewMode viewMode, ReportZoomMode zoomMode, int zoomPercent) {
        this.fontFamily = fontFamily;
        this.fontSizeStep = fontSizeStep;
        this.lineSpacing = lineSpacing;
        this.autoFit = autoFit;
        this.thumbnailsVisible = thumbnailsVisible;
        this.viewMode = viewMode;
        this.zoomMode = zoomMode;
        this.zoomPercent = zoomPercent;
    }
}
