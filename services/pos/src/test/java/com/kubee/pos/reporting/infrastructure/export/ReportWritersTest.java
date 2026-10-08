package com.kubee.pos.reporting.infrastructure.export;

import com.kubee.pos.reporting.application.export.ReportTable;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportWritersTest {

    private static final List<ReportTable> TABLES = List.of(
            new ReportTable("Cancelled orders", List.of("Order", "Reason", "Cancelled", "Value"), List.of(
                    Arrays.asList("12", "=HYPERLINK(\"x\")", LocalDateTime.of(2026, 10, 5, 21, 7), new BigDecimal("80.00")),
                    Arrays.asList("13", "late, cold \"tea\"", null, new BigDecimal("1234.50")))),
            new ReportTable("Refunds", List.of("Order", "Amount"), List.of(Arrays.asList("3", new BigDecimal("20.00")))));

    @Test
    void csvHasBomSectionsEscapingAndNoFormulas() {
        String csv = new String(new CsvReportWriter().write(TABLES), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿Cancelled orders\r\nOrder,Reason,Cancelled,Value\r\n");
        assertThat(csv).contains("12,\"'=HYPERLINK(\"\"x\"\")\",05-10-2026 21:07,80.00\r\n");
        assertThat(csv).contains("13,\"late, cold \"\"tea\"\"\",,1234.50\r\n");
        assertThat(csv).contains("\r\n\r\nRefunds\r\nOrder,Amount\r\n3,20.00\r\n");
    }

    @Test
    void xlsxHasOneSheetPerTableWithNumericAmounts() throws Exception {
        byte[] file = new XlsxReportWriter().write(TABLES);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            Sheet orders = workbook.getSheet("Cancelled orders");
            assertThat(orders.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Reason");
            assertThat(orders.getRow(1).getCell(1).getStringCellValue()).isEqualTo("'=HYPERLINK(\"x\")");
            assertThat(orders.getRow(2).getCell(3).getNumericCellValue()).isEqualTo(1234.5);
            assertThat(workbook.getSheet("Refunds").getRow(1).getCell(1).getNumericCellValue()).isEqualTo(20.0);
        }
    }
}
