package org.church.steward.ui.view.datamanagement.recordimport;

import java.util.Set;

import javax.swing.JOptionPane;

import org.church.steward.domain.backup.controller.BackupController;
import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.datatransfer.controller.RecordImportController;
import org.church.steward.domain.datatransfer.controller.SpreadsheetFileController;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.ui.view.datamanagement.importing.AbstractImportPanel;
import org.church.steward.ui.view.datamanagement.importing.CommitStepPanel;
import org.church.steward.ui.view.datamanagement.importing.FileStepPanel;
import org.church.steward.ui.view.datamanagement.importing.ImportSession;
import org.church.steward.ui.view.datamanagement.recordimport.RecordImportSession.AnalysisChange;
import org.church.steward.ui.view.datamanagement.recordimport.RecordImportSession.SelectionSummary;

import lombok.extern.slf4j.Slf4j;

/**
 * 수입/지출 가져오기를 단계별로 진행하는 패널. 수입/지출 탭 안에 놓인다.
 * <p>
 * 단계: 파일 선택 → 열 지정 → 예산·관계자 매핑 → 검토 및 선택 → 백업 및 반영.
 * 진행 상태는 {@link RecordImportSession}이 가지며, 매핑을 바꾸거나 다른 메뉴에 다녀오면 현재 데이터로 다시
 * 분석한다.
 */
@Slf4j
public class RecordImportPanel extends AbstractImportPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] STEP_NAMES = { "파일 선택", "열 지정", "매핑", "검토 및 선택", "백업 및 반영" };
    private static final int STEP_FILE = 0;
    private static final int STEP_COLUMNS = 1;
    private static final int STEP_MATCHING = 2;
    private static final int STEP_REVIEW = 3;
    private static final int STEP_COMMIT = 4;

    private final RecordImportController recordImportController;

    private final RecordImportSession session = new RecordImportSession();
    private final FileStepPanel fileStepPanel;
    private final ColumnMappingStepPanel columnMappingStepPanel;
    private final MatchingStepPanel matchingStepPanel;
    private final RecordReviewStepPanel reviewStepPanel;
    private final CommitStepPanel commitStepPanel;

    /**
     * @param onClosed 가져오기를 마쳤거나 취소해서 이 패널을 닫아야 할 때 실행할 동작
     */
    public RecordImportPanel(SpreadsheetFileController spreadsheetFileController,
            RecordImportController recordImportController, BackupController backupController,
            BudgetController budgetController, ContactController contactController,
            ContactGroupController contactGroupController, Runnable onClosed) {
        super("수입/지출 가져오기", STEP_NAMES, onClosed);
        this.recordImportController = recordImportController;

        this.fileStepPanel = new FileStepPanel(session, spreadsheetFileController, this::updateNavigation);
        this.columnMappingStepPanel = new ColumnMappingStepPanel(session);
        // 매핑을 바꾸면 그 값을 쓰는 행들의 상태가 달라지므로 다시 분석한다.
        this.matchingStepPanel = new MatchingStepPanel(session, budgetController, contactController,
                contactGroupController, () -> analyze(false, () -> {
                }));
        this.reviewStepPanel = new RecordReviewStepPanel(session, this::updateNavigation);
        this.commitStepPanel = new CommitStepPanel(session, backupController, this::createCommitSummary,
                this::updateNavigation);

        addStep(fileStepPanel);
        addStep(columnMappingStepPanel);
        addStep(matchingStepPanel);
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
        matchingStepPanel.reset();
    }

    @Override
    protected boolean canProceed(int step) {
        return switch (step) {
        case STEP_FILE -> session.hasData();
        case STEP_MATCHING -> session.hasAnalysis();
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
            columnMappingStepPanel.load();
            showStep(STEP_COLUMNS);
        }
        case STEP_COLUMNS -> {
            columnMappingStepPanel.store();
            String problem = session.validateMapping();
            if (problem != null) {
                showMessage(problem, JOptionPane.ERROR_MESSAGE);
                return;
            }
            analyze(false, () -> showStep(STEP_MATCHING));
        }
        case STEP_MATCHING -> {
            if (!confirmUnmapped()) {
                return;
            }
            showStep(STEP_REVIEW);
        }
        case STEP_REVIEW -> {
            commitStepPanel.refresh();
            showStep(STEP_COMMIT);
        }
        default -> commit();
        }
    }

    /**
     * 매핑되지 않은 값이 남아 있으면, 그 값을 쓰는 행은 가져오지 않게 된다는 것을 알리고 계속할지 묻는다.
     */
    private boolean confirmUnmapped() {
        int unmappedCount = session.countUnmapped();
        if (unmappedCount == 0) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                String.format("매핑되지 않은 값이 %,d개 있습니다.\n이 값을 쓰는 행은 오류로 표시되어 가져올 수 없습니다.\n그대로 다음 단계로 갈까요?", unmappedCount),
                "수입/지출 가져오기", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return answer == JOptionPane.YES_OPTION;
    }

    /**
     * 파일 선택과 열 지정 단계에서는 다음 단계로 넘어갈 때 어차피 새로 분석하므로, 분석 결과를 보고 있는 단계에서만
     * 다시 분석한다. 사용자가 정한 것(파일, 열 지정, 직접 한 매핑과 선택)은 유지한다.
     */
    @Override
    protected void onReturnedToStep(int step) {
        if (step >= STEP_MATCHING && session.hasAnalysis()) {
            analyze(true, () -> {
            });
        }
    }

    /**
     * 현재 데이터와 지금의 매핑으로 분석해 세션에 담는다.
     *
     * @param afterReturn 다른 메뉴에 다녀온 뒤의 재분석인지 여부. 이때는 행의 상태가 달라진 것을 알리고,
     *                    백업 단계에 있었으면 달라진 내용을 다시 확인하도록 검토 단계로 돌린다.
     * @param onAnalyzed  분석에 성공한 뒤 실행할 동작
     */
    private void analyze(boolean afterReturn, Runnable onAnalyzed) {
        Long fiscalYearId = session.getFiscalYear().getId();
        RecordImportRequestDto request = session.toRequest();

        runTask(() -> recordImportController.analyze(fiscalYearId, request),
                analysis -> {
                    AnalysisChange change = session.applyAnalysis(analysis);
                    matchingStepPanel.refresh();
                    reviewStepPanel.refresh();
                    setNotice(createNotice(afterReturn, change));
                    if (afterReturn && change.hasChanges() && getStep() > STEP_REVIEW) {
                        showStep(STEP_REVIEW);
                    }
                    updateNavigation();
                    onAnalyzed.run();
                },
                err -> {
                    log.error("Failed to analyze record import", err);
                    if (afterReturn) {
                        // 회계 연도가 마감된 경우 등. 마감을 취소하고 돌아오면 이어서 할 수 있다.
                        showStep(Math.min(getStep(), STEP_REVIEW));
                        setNotice("다시 분석하지 못했습니다: " + err.getMessage());
                    } else {
                        showMessage(err.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                });
    }

    /**
     * 다시 분석한 결과로 알릴 문구를 만든다. 알릴 것이 없으면 null이다.
     * 직접 한 매핑이 해제된 것은 사용자가 다시 매핑해야 하므로 언제나 알리고,
     * 행의 상태가 달라진 것은 다른 메뉴에 다녀온 경우에만 알린다(매핑을 바꿔서 달라진 것은 사용자가 이미 안다).
     */
    private String createNotice(boolean afterReturn, AnalysisChange change) {
        if (afterReturn && change.hasChanges()) {
            return String.format("다른 메뉴에서 바뀐 데이터를 반영해 다시 분석했습니다. 상태가 달라진 행 %,d건, 해제된 매핑 %,d건.",
                    change.changedItemCount(), change.releasedMappingCount());
        }
        if (change.releasedMappingCount() > 0) {
            return String.format("매핑한 대상을 더는 쓸 수 없어 매핑 %,d건이 해제되었습니다.", change.releasedMappingCount());
        }
        return null;
    }

    private void commit() {
        Long fiscalYearId = session.getFiscalYear().getId();
        RecordImportRequestDto request = session.toRequest();
        Set<String> selectedKeys = Set.copyOf(session.getSelectedKeys());
        String signature = session.getAnalysis().getSignature();
        long backupLogId = session.getBackupLogId();
        String fileName = session.getFile().getFileName().toString();

        runTask(() -> recordImportController.importRecords(fiscalYearId, request, selectedKeys, signature,
                backupLogId, fileName),
                result -> {
                    showMessage(String.format("수입/지출 내역 %,d건을 가져왔습니다.", result.getCreatedCount()),
                            JOptionPane.INFORMATION_MESSAGE);
                    close();
                },
                err -> {
                    log.error("Failed to import financial records", err);
                    handleCommitError(err, commitStepPanel::refresh, () -> analyze(true, () -> showStep(STEP_REVIEW)));
                });
    }

    private String createCommitSummary() {
        SelectionSummary summary = session.summarize();
        return String.format("회계 연도 \"%s\"에 수입 %,d건 %,d원, 지출 %,d건 %,d원을 등록합니다.",
                session.getFiscalYear().getName(), summary.incomeCount(), summary.incomeAmount(),
                summary.expenseCount(), summary.expenseAmount());
    }

}
