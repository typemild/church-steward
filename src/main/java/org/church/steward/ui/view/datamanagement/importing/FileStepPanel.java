package org.church.steward.ui.view.datamanagement.importing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.church.steward.domain.datatransfer.controller.SpreadsheetFileController;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetPasswordException;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetReader;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

import lombok.extern.slf4j.Slf4j;

/**
 * 가져오기의 1단계. 가져올 파일을 고르고, 시트(엑셀) 또는 인코딩(CSV)과 머리글 행을 정한다.
 * 파일은 여기서 읽어 {@link ImportSession}에 담아 두며, 이후 단계는 디스크의 파일을 다시 읽지 않는다.
 * 예산 가져오기와 수입/지출 가져오기가 함께 쓴다.
 */
@Slf4j
public class FileStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 미리보기에 보여주는 행의 수 */
    private static final int PREVIEW_ROWS = 50;
    private static final int ROW_NUMBER_COLUMN_WIDTH = 45;

    /**
     * CSV의 인코딩을 고르는 목록의 항목.
     *
     * @param charset 인코딩. 자동으로 판별하게 하면 null
     */
    private record CharsetOption(String label, Charset charset) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final CharsetOption[] CHARSET_OPTIONS = {
            new CharsetOption("자동 판별", null),
            new CharsetOption("UTF-8", StandardCharsets.UTF_8),
            new CharsetOption("MS949 (한글 윈도우)", SpreadsheetReader.FALLBACK_CHARSET) };

    /**
     * 파일에서 읽은 결과.
     */
    private record Loaded(boolean excel, List<String> sheetNames, String sheetName, SpreadsheetData data) {
    }

    private final ImportSession session;
    private final SpreadsheetFileController spreadsheetFileController;
    private final Runnable onChanged;

    private final StyledTextField fileField = new StyledTextField(ComponentSize.MEDIUM);
    private final SecondaryButton browseButton = new SecondaryButton("찾아보기", ComponentSize.MEDIUM);
    private final FieldLabel sheetLabel = new FieldLabel("시트", ComponentSize.MEDIUM, true);
    private final StyledComboBox<String> sheetCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final FieldLabel charsetLabel = new FieldLabel("인코딩", ComponentSize.MEDIUM, true);
    private final StyledComboBox<CharsetOption> charsetCombo = new StyledComboBox<>(CHARSET_OPTIONS,
            ComponentSize.MEDIUM);
    private final JSpinner headerRowSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1, 1));
    private final SubFieldLabel infoLabel = new SubFieldLabel(" ", ComponentSize.MEDIUM);
    private final JTable previewTable = new JTable();

    /** 화면의 값을 코드로 바꾸는 동안에는 리스너가 반응하지 않게 한다. */
    private boolean updating = false;
    private File lastDirectory;

    /**
     * @param onChanged 파일을 새로 읽었거나 머리글 행을 바꿨을 때 실행할 동작
     */
    public FileStepPanel(ImportSession session, SpreadsheetFileController spreadsheetFileController,
            Runnable onChanged) {
        this.session = session;
        this.spreadsheetFileController = spreadsheetFileController;
        this.onChanged = onChanged;

        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        fileField.setEditable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new FieldLabel("파일", ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        formPanel.add(fileField, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.gridx = 2;
        gbc.gridy = 0;
        formPanel.add(browseButton, gbc);

        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        optionPanel.setOpaque(false);
        sheetCombo.setPreferredSize(new Dimension(170, sheetCombo.getPreferredSize().height));
        optionPanel.add(sheetLabel);
        optionPanel.add(Box.createHorizontalStrut(8));
        optionPanel.add(sheetCombo);
        optionPanel.add(charsetLabel);
        optionPanel.add(Box.createHorizontalStrut(8));
        optionPanel.add(charsetCombo);
        optionPanel.add(Box.createHorizontalStrut(20));
        optionPanel.add(new FieldLabel("머리글 행 (없으면 0)", ComponentSize.MEDIUM, true));
        optionPanel.add(Box.createHorizontalStrut(8));
        optionPanel.add(headerRowSpinner);
        hideSpinnerFocusIndicator();

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        formPanel.add(optionPanel, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        formPanel.add(infoLabel, gbc);

        add(formPanel, BorderLayout.NORTH);

        previewTable.setRowHeight(Math.round(UIFonts.FONT_SIZE_BASE * 2.0f));
        previewTable.setFont(new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE));
        previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        previewTable.setRowSelectionAllowed(false);
        previewTable.getTableHeader().setReorderingAllowed(false);
        previewTable.setDefaultRenderer(Object.class, new PreviewCellRenderer());
        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        add(scrollPane, BorderLayout.CENTER);

        browseButton.addActionListener(_ -> chooseFile());
        sheetCombo.addActionListener(_ -> {
            if (!updating && session.hasData() && sheetCombo.getSelectedItem() != null) {
                load(session.getFile(), session.getPassword(), (String) sheetCombo.getSelectedItem(), null);
            }
        });
        charsetCombo.addActionListener(_ -> {
            if (!updating && session.hasData()) {
                CharsetOption option = (CharsetOption) charsetCombo.getSelectedItem();
                load(session.getFile(), null, null, option.charset());
            }
        });
        headerRowSpinner.addChangeListener(_ -> {
            if (!updating) {
                session.setHeaderRowNumber((Integer) headerRowSpinner.getValue());
                previewTable.repaint();
                this.onChanged.run();
            }
        });

        refresh();
    }

    /**
     * 머리글 행으로 지정된 행을 굵게 보여준다. 행 번호 열은 흐리게 보여준다.
     */
    private final class PreviewCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            boolean headerRow = row == session.getHeaderRowNumber() - 1;
            setFont(getFont().deriveFont(headerRow ? Font.BOLD : Font.PLAIN));
            setBackground(headerRow ? UIColors.CHIP_BACKGROUND : table.getBackground());
            setForeground(column == 0 ? UIColors.TEXT_SUB : UIColors.TEXT_MAIN);
            setToolTipText(value == null ? null : value.toString());
            return this;
        }
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle("가져올 파일 선택");
        chooser.setFileFilter(new FileNameExtensionFilter("엑셀, CSV (*.xlsx, *.xls, *.csv)", "xlsx", "xls", "xlsm",
                "csv", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        lastDirectory = chooser.getSelectedFile().getParentFile();
        load(chooser.getSelectedFile().toPath(), null, null, null);
    }

    /**
     * 파일을 읽어 세션에 담는다. 암호가 필요하면 암호를 물어 다시 읽는다.
     *
     * @param sheetName 읽을 시트. null이거나 파일에 없는 시트이면 첫 번째 시트
     * @param charset   CSV의 인코딩. null이면 자동 판별
     */
    private void load(Path file, String password, String sheetName, Charset charset) {
        AsyncTask.run(browseButton,
                () -> {
                    boolean excel = spreadsheetFileController.isExcel(file);
                    List<String> sheetNames = excel ? spreadsheetFileController.listSheets(file, password)
                            : List.of();
                    String sheet = null;
                    if (excel) {
                        sheet = sheetName != null && sheetNames.contains(sheetName) ? sheetName
                                : sheetNames.getFirst();
                    }
                    return new Loaded(excel, sheetNames, sheet,
                            spreadsheetFileController.read(file, password, sheet, charset));
                },
                loaded -> {
                    session.setData(file, password, loaded.excel(), loaded.sheetNames(), loaded.sheetName(), charset,
                            loaded.data());
                    refresh();
                    onChanged.run();
                },
                err -> {
                    if (err instanceof SpreadsheetPasswordException) {
                        String enteredPassword = askPassword(err.getMessage());
                        if (enteredPassword != null) {
                            load(file, enteredPassword, sheetName, charset);
                        }
                        return;
                    }
                    log.error("Failed to read import file", err);
                    JOptionPane.showMessageDialog(this, err.getMessage(), "파일 읽기", JOptionPane.ERROR_MESSAGE);
                });
    }

    /**
     * @return 입력한 암호. 취소했으면 null
     */
    private String askPassword(String message) {
        ValidatedPasswordField passwordField = new ValidatedPasswordField(20, ComponentSize.MEDIUM);
        int answer = JOptionPane.showConfirmDialog(this, new Object[] { message, passwordField }, "파일 암호",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return answer == JOptionPane.OK_OPTION ? new String(passwordField.getPassword()) : null;
    }

    /**
     * 세션의 내용을 화면에 반영한다.
     */
    public void refresh() {
        updating = true;
        try {
            boolean hasData = session.hasData();
            fileField.setText(hasData ? session.getFile().toString() : "");
            fileField.setCaretPosition(0);

            boolean excel = hasData && session.isExcel();
            sheetLabel.setVisible(excel);
            sheetCombo.setVisible(excel);
            sheetCombo.setModel(new DefaultComboBoxModel<>(session.getSheetNames().toArray(new String[0])));
            sheetCombo.setSelectedItem(session.getSheetName());

            boolean csv = hasData && !session.isExcel();
            charsetLabel.setVisible(csv);
            charsetCombo.setVisible(csv);
            for (CharsetOption option : CHARSET_OPTIONS) {
                if (Objects.equals(option.charset(), session.getCharset())) {
                    charsetCombo.setSelectedItem(option);
                }
            }

            int rowCount = hasData ? session.getData().rows().size() : 1;
            headerRowSpinner.setModel(new SpinnerNumberModel(Math.min(session.getHeaderRowNumber(), rowCount), 0,
                    rowCount, 1));
            hideSpinnerFocusIndicator();
            headerRowSpinner.setEnabled(hasData);

            infoLabel.setText(createInfoText());
            fillPreview();
        } finally {
            updating = false;
        }
    }

    private String createInfoText() {
        if (!session.hasData()) {
            return "가져올 엑셀(xlsx, xls) 또는 CSV 파일을 선택해주세요.";
        }
        SpreadsheetData data = session.getData();
        String info = String.format("전체 %,d행, %d열", data.rows().size(), data.columnCount());
        if (data.charset() != null) {
            info += " / 읽은 인코딩: " + data.charset().displayName();
        }
        if (data.rows().size() > PREVIEW_ROWS) {
            info += " / 아래에는 앞의 " + PREVIEW_ROWS + "행만 보여줍니다.";
        }
        return info;
    }

    private void fillPreview() {
        DefaultTableModel model = new DefaultTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addColumn("행");
        for (int column = 0; column < session.getColumnCount(); column++) {
            model.addColumn(ImportSession.toColumnLetter(column));
        }
        if (session.hasData()) {
            List<List<Object>> rows = session.getData().rows();
            for (int i = 0; i < Math.min(rows.size(), PREVIEW_ROWS); i++) {
                Object[] values = new Object[rows.get(i).size() + 1];
                values[0] = i + 1;
                for (int column = 0; column < rows.get(i).size(); column++) {
                    values[column + 1] = rows.get(i).get(column);
                }
                model.addRow(values);
            }
        }
        previewTable.setModel(model);
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(ROW_NUMBER_COLUMN_WIDTH);
        for (int column = 1; column < previewTable.getColumnCount(); column++) {
            previewTable.getColumnModel().getColumn(column).setPreferredWidth(110);
        }
    }

    /**
     * 머리글 행 입력란의 포커스 테두리를 숨긴다. 모델을 바꾸면 입력 필드가 새로 만들어지므로 그때마다 다시 호출한다.
     */
    private void hideSpinnerFocusIndicator() {
        if (headerRowSpinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            FocusIndicator.hide(editor.getTextField());
        }
    }

}
