package org.church.steward.domain.datatransfer.service.impl;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.church.steward.domain.datatransfer.service.SpreadsheetFileService;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetReader;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpreadsheetFileServiceImpl implements SpreadsheetFileService {

    private final SpreadsheetReader spreadsheetReader;

    @Override
    public boolean isExcel(Path file) {
        return spreadsheetReader.isExcel(file);
    }

    @Override
    public List<String> listSheets(Path file, String password) {
        return spreadsheetReader.listSheets(file, password);
    }

    @Override
    public SpreadsheetData read(Path file, String password, String sheetName, Charset charset) {
        return spreadsheetReader.read(file, password, sheetName, charset);
    }

}
