package org.church.steward.ui.view.datamanagement.budgetimport;

import java.util.Set;

import javax.swing.JOptionPane;

import org.church.steward.domain.backup.controller.BackupController;
import org.church.steward.domain.datatransfer.controller.BudgetImportController;
import org.church.steward.domain.datatransfer.controller.SpreadsheetFileController;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.ui.view.datamanagement.budgetimport.BudgetImportSession.SelectionSummary;
import org.church.steward.ui.view.datamanagement.importing.AbstractImportPanel;
import org.church.steward.ui.view.datamanagement.importing.CommitStepPanel;
import org.church.steward.ui.view.datamanagement.importing.FileStepPanel;
import org.church.steward.ui.view.datamanagement.importing.ImportSession;

import lombok.extern.slf4j.Slf4j;

/**
 * 예산 가져오기를 단계별로 진행하는 패널. 예산 탭 안에 놓인다.
 * <p>
 * 단계: 파일 선택 → 열 지정 → 검토 및 선택 → 백업 및 반영.
 * 진행 상태는 {@link BudgetImportSession}이 가지며, 다른 메뉴에 다녀오면 현재 데이터로 다시 분석한다.
 */
@Slf4j
public class BudgetImportPanel extends AbstractImportPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] STEP_NAMES = { "파일 선택", "열 지정", "검토 및 선택", "백업 및 반영" };
    private static final int STEP_FILE = 0;
    private static final int STEP_MAPPING = 1;
    private static final int STEP_REVIEW = 2;
    private static final int STEP_COMMIT = 3;

    private final BudgetImportController budgetImportController;

    private final BudgetImportSession session = new BudgetImportSession();
    private final FileStepPanel fileStepPanel;
    private final MappingStepPanel mappingStepPanel;
    private final ReviewStepPanel reviewStepPanel;
    private final CommitStepPanel commitStepPanel;

    /**
     * @param onClosed 가져오기를 마쳤거나 취소해서 이 패널을 닫아야 할 때 실행할 동작
     */
    public BudgetImportPanel(SpreadsheetFileController spreadsheetFileController,
            BudgetImportController budgetImportController, BackupController backupController, Runnable onClosed) {
        super("예산 가져오기", STEP_NAMES, onClosed);
        this.budgetImportController = budgetImportController;

        this.fileStepPanel = new FileStepPanel(session, spreadsheetFileController, this::updateNavigation);
        this.mappingStepPanel = new MappingStepPanel(session);
        this.reviewStepPanel = new ReviewStepPanel(session, this::updateNavigation);
        this.commitStepPanel = new CommitStepPanel(session, backupController, this::createCommitSummary,
                this::updateNavigation);

        addStep(fileStepPanel);
        addStep(mappingStepPanel);
        addStep(reviewStepPanel);
        addStep(commitStepPanel);
    }

    @Override
    protected ImportSession getSession() {
        return session;
    }

    @Override
    protected void onStarted() {
        fileStepPanel.refresh();
    }

    @Override
    protected boolean canProceed(int step) {
        return switch (step) {
        case STEP_FILE -> session.hasData();
        case STEP_REVIEW -> session.hasAnalysis() && !session.getSelectedKeys().isEmpty();
        case STEP_COMMIT -> session.hasBackup() && !session.getSelectedKeys().isEmpty();
        default -> true;
        };
    }

    @Override
    protected void onNext(int step) {
        switch (step) {
        case STEP_FILE -> {
            if (session.getDataRowCount() == 0) {
                showMessage("머리글 아래에 가져올 행이 없습니다.", JOptionPane.ERROR_MESSAGE);
                return;
            }
            mappingStepPanel.load();
            showStep(STEP_MAPPING);
        }
        case STEP_MAPPING -> {
            mappingStepPanel.store();
            String problem = session.validateMapping();
            if (problem != null) {
                showMessage(problem, JOptionPane.ERROR_MESSAGE);
                return;
            }
            analyze(false, () -> showStep(STEP_REVIEW));
        }
        case STEP_REVIEW -> {
            commitStepPanel.refresh();
            showStep(STEP_COMMIT);
        }
        default -> commit();
        }
    }

    /**
     * 파일 선택과 열 지정 단계에서는 다음 단계로 넘어갈 때 어차피 새로 분석하므로, 분석 결과를 보고 있는 단계에서만
     * 다시 분석한다. 사용자가 정한 것(파일, 열 지정, 직접 바꾼 선택)은 유지한다.
     */
    @Override
    protected void onReturnedToStep(int step) {
        if (step >= STEP_REVIEW && session.hasAnalysis()) {
            analyze(true, () -> {
            });
        }
    }

    /**
     * 현재 데이터로 분석해 세션에 담는다.
     *
     * @param reportChanges 이전 분석 결과와 달라진 것을 알릴지 여부. 다른 메뉴에 다녀온 뒤의 재분석에서만 알린다.
     * @param onAnalyzed    분석에 성공한 뒤 실행할 동작
     */
    private void analyze(boolean reportChanges, Runnable onAnalyzed) {
        Long fiscalYearId = session.getFiscalYear().getId();
        BudgetImportRequestDto request = session.toRequest();

        runTask(() -> budgetImportController.analyze(fiscalYearId, request),
                analysis -> {
                    int changedCount = session.applyAnalysis(analysis);
                    reviewStepPanel.refresh();
                    if (reportChanges && changedCount > 0) {
                        // 백업 단계까지 갔더라도 달라진 내용을 다시 확인하도록 검토 단계로 돌린다.
                        showStep(STEP_REVIEW);
                        setNotice(String.format("다른 메뉴에서 바뀐 데이터를 반영해 다시 분석했습니다. 상태가 달라진 건이 %,d건 있습니다.",
                                changedCount));
                    } else if (!reportChanges) {
                        setNotice(null);
                    }
                    updateNavigation();
                    onAnalyzed.run();
                },
                err -> {
                    log.error("Failed to analyze budget import", err);
                    if (reportChanges) {
                        // 회계 연도가 마감된 경우 등. 마감을 취소하고 돌아오면 이어서 할 수 있다.
                        showStep(Math.min(getStep(), STEP_REVIEW));
                        setNotice("다시 분석하지 못했습니다: " + err.getMessage());
                    } else {
                        showMessage(err.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                });
    }

    private void commit() {
        Long fiscalYearId = session.getFiscalYear().getId();
        BudgetImportRequestDto request = session.toRequest();
        Set<String> selectedKeys = Set.copyOf(session.getSelectedKeys());
        String signature = session.getAnalysis().getSignature();
        long backupLogId = session.getBackupLogId();
        String fileName = session.getFile().getFileName().toString();

        runTask(() -> budgetImportController.importBudgets(fiscalYearId, request, selectedKeys, signature,
                backupLogId, fileName),
                result -> {
                    showMessage(String.format("예산을 가져왔습니다.\n추가 %,d건, 금액 갱신 %,d건", result.getCreatedCount(),
                            result.getUpdatedCount()), JOptionPane.INFORMATION_MESSAGE);
                    close();
                },
                err -> {
                    log.error("Failed to import budgets", err);
                    handleCommitError(err, commitStepPanel::refresh, () -> analyze(true, () -> showStep(STEP_REVIEW)));
                });
    }

    private String createCommitSummary() {
        SelectionSummary summary = session.summarize();
        return String.format("회계 연도 \"%s\"에 예산 추가 %,d건, 금액 갱신 %,d건을 반영합니다.",
                session.getFiscalYear().getName(), summary.createCount(), summary.updateCount());
    }

}
