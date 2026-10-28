package org.church.steward.ui.view.report.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.church.steward.domain.report.entity.ReportFontFamily;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportAutoFitterTest {

    /** 기본 서식(나눔고딕, 기본 크기, 행 간격 130%) */
    private static final ReportStyle DEFAULT = ReportStyle.DEFAULT;

    /**
     * 가장 작은 글자가 7pt인 문서를 지정한 페이지 수로 만든다. 가장 작게 줄일 수 있는 글자 크기 조정 단계는 -2(6pt)다.
     */
    private static ReportDocument document(ReportStyle style, int pageCount) {
        ReportCanvas canvas = new ReportCanvas("시험", style);
        for (int i = 0; i < pageCount; i++) {
            if (i > 0) {
                canvas.newPage();
            }
            canvas.text("본문", style.regular(ReportFonts.SIZE_SMALL), ReportAlign.LEFT, canvas.getLeft(),
                    canvas.getY() + 10);
        }
        return canvas.finish();
    }

    /**
     * 행 간격과 글자 크기 조정 단계가 모두 기준 이하일 때만 한 페이지에 담기는 가짜 보고서
     */
    private static Function<ReportStyle, ReportDocument> composer(int maxLineSpacing, int maxFontSizeStep,
            List<ReportStyle> composed) {
        return style -> {
            composed.add(style);
            boolean fits = style.lineSpacingPercent() <= maxLineSpacing && style.fontSizeStep() <= maxFontSizeStep;
            return document(style, fits ? 1 : 2);
        };
    }

    private static ReportStyle style(int fontSizeStep, int lineSpacing) {
        return DEFAULT.withFontSizeStep(fontSizeStep)
                .withLineSpacing(lineSpacing);
    }

    @Nested
    @DisplayName("fit 테스트")
    class FitTest {

        @Test
        @DisplayName("기본 서식으로 한 페이지에 담기면 서식을 바꾸지 않고 한 번만 배치한다")
        void fit_DefaultFits_KeepsDefaultStyle() {
            // Given
            List<ReportStyle> composed = new ArrayList<>();

            // When
            ReportAutoFitter.Result result = ReportAutoFitter.fit(composer(200, 10, composed),
                    ReportFontFamily.NANUM_GOTHIC);

            // Then
            assertThat(result.fits()).isTrue();
            assertThat(result.document()
                    .getStyle()).isEqualTo(DEFAULT);
            assertThat(composed).hasSize(1);
        }

        @Test
        @DisplayName("고른 글꼴은 그대로 두고 기본 크기와 행 간격에서 시작한다")
        void fit_Myeongjo_StartsFromDefaultWithChosenFont() {
            // Given
            List<ReportStyle> composed = new ArrayList<>();

            // When
            ReportAutoFitter.fit(composer(200, 10, composed), ReportFontFamily.NANUM_MYEONGJO);

            // Then
            assertThat(composed.getFirst()).isEqualTo(DEFAULT.withFontFamily(ReportFontFamily.NANUM_MYEONGJO));
        }

        @Test
        @DisplayName("행 간격을 120%로 줄여 담기면 글자 크기는 줄이지 않는다")
        void fit_FirstLineSpacingIsEnough_ReducesLineSpacingOnly() {
            // Given
            List<ReportStyle> composed = new ArrayList<>();

            // When
            ReportAutoFitter.Result result = ReportAutoFitter.fit(composer(120, 0, composed),
                    ReportFontFamily.NANUM_GOTHIC);

            // Then
            assertThat(result.fits()).isTrue();
            assertThat(result.document()
                    .getStyle()).isEqualTo(style(0, 120));
        }

        @Test
        @DisplayName("행 간격 120%로도 넘치면 행 간격은 120%에 두고 담기는 가장 큰 글자 크기를 고른다")
        void fit_FirstLineSpacingNotEnough_ReducesFontSizeAt120() {
            // Given
            List<ReportStyle> composed = new ArrayList<>();

            // When
            ReportAutoFitter.Result result = ReportAutoFitter.fit(composer(120, -1, composed),
                    ReportFontFamily.NANUM_GOTHIC);

            // Then
            assertThat(result.fits()).isTrue();
            assertThat(result.document()
                    .getStyle()).isEqualTo(style(-1, 120));
        }

        @Test
        @DisplayName("글자 크기를 가장 작게 줄여도 넘치면 그 크기에서 행 간격을 110%, 100% 순으로 줄인다")
        void fit_SmallestFontNotEnough_ReducesLineSpacingFurther() {
            // Given
            List<ReportStyle> composed = new ArrayList<>();

            // When
            ReportAutoFitter.Result result = ReportAutoFitter.fit(composer(110, -2, composed),
                    ReportFontFamily.NANUM_GOTHIC);

            // Then
            assertThat(result.fits()).isTrue();
            assertThat(result.document()
                    .getStyle()).isEqualTo(style(-2, 110));
        }

        @Test
        @DisplayName("기본 서식부터 줄이는 순서대로 하나씩 배치하고, 처음으로 한 페이지에 담기면 더 배치하지 않는다")
        void fit_Fits_ComposesInOrderAndStopsAtFirstFit() {
            // Given
            List<ReportStyle> composed = new ArrayList<>();

            // When
            ReportAutoFitter.fit(composer(120, -1, composed), ReportFontFamily.NANUM_GOTHIC);

            // Then
            assertThat(composed).containsExactly(DEFAULT, style(0, 120), style(-1, 120));
        }

        @Test
        @DisplayName("가장 작게 줄여도 넘치면 담기지 않았다고 알리고 가장 작게 줄인 문서를 돌려준다")
        void fit_NeverFits_ReturnsSmallestStyle() {
            // Given
            List<ReportStyle> composed = new ArrayList<>();

            // When
            ReportAutoFitter.Result result = ReportAutoFitter.fit(composer(100, -10, composed),
                    ReportFontFamily.NANUM_GOTHIC);

            // Then
            assertThat(result.fits()).isFalse();
            assertThat(result.document()
                    .getStyle()).isEqualTo(style(-2, 100));
        }
    }

    @Nested
    @DisplayName("candidates 테스트")
    class CandidatesTest {

        @Test
        @DisplayName("행 간격을 120%로 줄이고, 그 행 간격에서 글자 크기를 줄인 뒤, 가장 작은 글자 크기에서 행 간격을 100%까지 줄인다")
        void candidates_FromDefault_FollowsReductionOrder() {
            // When
            List<ReportStyle> candidates = ReportAutoFitter.candidates(DEFAULT, -2);

            // Then
            assertThat(candidates).containsExactly(
                    style(0, 120),
                    style(-1, 120),
                    style(-2, 120),
                    style(-2, 110),
                    style(-2, 100));
        }

        @Test
        @DisplayName("글자 크기를 더 줄일 수 없으면 행 간격만 100%까지 줄인다")
        void candidates_NoFontReduction_ReducesLineSpacingOnly() {
            // When
            List<ReportStyle> candidates = ReportAutoFitter.candidates(DEFAULT, 0);

            // Then
            assertThat(candidates).containsExactly(
                    style(0, 120),
                    style(0, 110),
                    style(0, 100));
        }
    }
}
