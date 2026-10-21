package org.church.steward.domain.datatransfer.controller;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.church.steward.domain.datatransfer.service.SpreadsheetFileService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.AdminAccessChecker;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class SpreadsheetFileController {

    private final SpreadsheetFileService spreadsheetFileService;
    private final AdminAccessChecker adminAccessChecker;

    public boolean isExcel(Path file) {
        adminAccessChecker.checkAdmin();
        return spreadsheetFileService.isExcel(file);
    }

    public List<String> listSheets(Path file, String password) {
        adminAccessChecker.checkAdmin();
        return spreadsheetFileService.listSheets(file, password);
    }

    public SpreadsheetData read(Path file, String password, String sheetName, Charset charset) {
        adminAccessChecker.checkAdmin();
        return spreadsheetFileService.read(file, password, sheetName, charset);
    }
}
