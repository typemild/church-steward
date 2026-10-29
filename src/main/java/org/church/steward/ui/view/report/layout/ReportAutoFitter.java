package org.church.steward.ui.view.report.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportSettingConstraints;

/**
 * 보고서가 한 페이지에 담기도록 행 간격과 글자 크기를 줄여 가며 다시 배치한다.
 * <p>
 * 사용자가 직접 정한 글자 크기와 행 간격과 무관하게 항상 기본 서식(글꼴만 사용자가 고른 것)에서 시작하며, 줄이기만 하고 키우지는
 * 않는다. 줄이는 순서는 다음과 같다. 글자를 읽기 좋게 두려고 행 간격을 먼저 조금 줄이고, 행 간격이 아주 좁아지는 것은 마지막으로
 * 미룬다.
 * <ol>
 * <li>행 간격을 {@value #FIRST_LINE_SPACING}%까지 한 단계씩 줄인다.</li>
 * <li>그래도 넘치면 그 행 간격에서 글자 크기를 가장 작은 글자가 허용 범위의 하한에 닿을 때까지 한 단계씩 줄인다.</li>
 * <li>그래도 넘치면 가장 작은 글자 크기에서 행 간격을 100%까지 한 단계씩 줄인다.</li>
 * </ol>
 * 후보를 이 순서대로 하나씩 배치해 보고 처음으로 한 페이지에 담기는 후보를 쓴다.
 */
public final class ReportAutoFitter {

    /** 글자 크기를 줄이기 전에 먼저 줄여 보는 행 간격의 하한(%) */
    static final int FIRST_LINE_SPACING = 120;

    /**
     * 맞춘 결과
     *
     * @param document 배치한 문서. 문서의 서식이 실제로 쓴 서식이다.
     * @param fits     한 페이지에 담겼는지 여부. 가장 작게 줄여도 넘치면 false이며 그때의 문서를 돌려준다.
     */
    public record Result(ReportDocument document, boolean fits) {
    }

    private ReportAutoFitter() {
        // 인스턴스화 방지
    }

    /**
     * @param composer   서식을 받아 문서를 배치하는 함수
     * @param fontFamily 사용자가 고른 글꼴. 글꼴은 줄이는 대상이 아니므로 그대로 쓴다.
     */
    public static Result fit(Function<ReportStyle, ReportDocument> composer, ReportFontFamily fontFamily) {
        ReportStyle start = ReportStyle.DEFAULT.withFontFamily(fontFamily);
        ReportDocument first = composer.apply(start);
        if (first.getPageCount() <= 1) {
            return new Result(first, true);
        }

        ReportDocument document = first;
        for (ReportStyle candidate : candidates(start, first.getMinFontSizeStep())) {
            document = composer.apply(candidate);
            if (document.getPageCount() <= 1) {
                return new Result(document, true);
            }
        }
        // 가장 작게 줄인 문서도 넘친다.
        return new Result(document, false);
    }

    /**
     * 시작 서식보다 작은 서식들을 줄이는 순서대로 나열한다.
     *
     * @param minFontSizeStep 가장 작은 글자가 허용 범위를 벗어나지 않는 가장 작은 조정 단계
     */
    static List<ReportStyle> candidates(ReportStyle start, int minFontSizeStep) {
        int step = ReportSettingConstraints.LINE_SPACING_STEP;
        List<ReportStyle> candidates = new ArrayList<>();

        // 1. 행 간격을 먼저 조금 줄인다.
        int lineSpacing = start.lineSpacingPercent();
        while (lineSpacing - step >= FIRST_LINE_SPACING) {
            lineSpacing -= step;
            candidates.add(start.withLineSpacing(lineSpacing));
        }

        // 2. 그 행 간격에서 글자 크기를 가장 작게까지 줄인다.
        ReportStyle current = start.withLineSpacing(lineSpacing);
        for (int fontSizeStep = start.fontSizeStep() - 1; fontSizeStep >= minFontSizeStep; fontSizeStep--) {
            current = current.withFontSizeStep(fontSizeStep);
            candidates.add(current);
        }

        // 3. 가장 작은 글자 크기에서 행 간격을 마저 줄인다.
        while (lineSpacing - step >= ReportSettingConstraints.LINE_SPACING_MIN) {
            lineSpacing -= step;
            candidates.add(current.withLineSpacing(lineSpacing));
        }
        return candidates;
    }
}
