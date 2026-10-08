package com.kubee.pos.reporting.infrastructure.export;

import com.kubee.pos.reporting.application.export.ExportFormat;
import com.kubee.pos.reporting.application.export.ReportFileWriter;
import com.kubee.pos.reporting.application.export.ReportTable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Excel workbook: one sheet per table, bold frozen header, amounts as real numbers (so Excel can sum them). */
@Component
class XlsxReportWriter implements ReportFileWriter {

    @Override
    public ExportFormat format() {
        return ExportFormat.XLSX;
    }

    @Override
    public byte[] write(List<ReportTable> tables) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);
            CellStyle amount = workbook.createCellStyle();
            amount.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Set<String> usedNames = new HashSet<>();
            for (ReportTable table : tables) {
                Sheet sheet = workbook.createSheet(sheetName(table.title(), usedNames));
                Row head = sheet.createRow(0);
                for (int c = 0; c < table.headers().size(); c++) {
                    Cell cell = head.createCell(c);
                    cell.setCellValue(table.headers().get(c));
                    cell.setCellStyle(header);
                }
                for (int r = 0; r < table.rows().size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    List<Object> values = table.rows().get(r);
                    for (int c = 0; c < values.size(); c++) {
                        Object value = values.get(c);
                        Cell cell = row.createCell(c);
                        switch (value) {
                            case null -> cell.setBlank();
                            case BigDecimal b -> {
                                cell.setCellValue(b.doubleValue());
                                cell.setCellStyle(amount);
                            }
                            case Number n -> cell.setCellValue(n.doubleValue());
                            default -> cell.setCellValue(CellText.of(value));
                        }
                    }
                }
                sheet.createFreezePane(0, 1);
                for (int c = 0; c < table.headers().size(); c++) {
                    sheet.setColumnWidth(c, Math.min(60, Math.max(12, table.headers().get(c).length() + 4)) * 256);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build the Excel file", e);
        }
    }

    /** Sheet names: max 31 characters, no []:*?/\ and unique. */
    private static String sheetName(String title, Set<String> used) {
        String base = WorkbookUtil.createSafeSheetName(title);
        String name = base;
        for (int i = 2; !used.add(name.toLowerCase()); i++) {
            String suffix = " " + i;
            name = base.substring(0, Math.min(base.length(), 31 - suffix.length())) + suffix;
        }
        return name;
    }
}
