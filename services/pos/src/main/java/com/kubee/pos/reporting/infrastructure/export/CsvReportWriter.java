package com.kubee.pos.reporting.infrastructure.export;

import com.kubee.pos.reporting.application.export.ExportFormat;
import com.kubee.pos.reporting.application.export.ReportFileWriter;
import com.kubee.pos.reporting.application.export.ReportTable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UTF-8 CSV (with BOM so Excel shows ₹ and Indian names correctly). Several tables are written one after
 * another: a title line, the header, the rows, then an empty line.
 */
@Component
class CsvReportWriter implements ReportFileWriter {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Override
    public ExportFormat format() {
        return ExportFormat.CSV;
    }

    @Override
    public byte[] write(List<ReportTable> tables) {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            ReportTable table = tables.get(i);
            if (tables.size() > 1) {
                csv.append(escape(table.title())).append("\r\n");
            }
            csv.append(line(table.headers().stream().map(h -> (Object) h).toList()));
            table.rows().forEach(row -> csv.append(line(row)));
            if (i < tables.size() - 1) {
                csv.append("\r\n");
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(BOM);
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static String line(List<Object> cells) {
        return cells.stream().map(CellText::of).map(CsvReportWriter::escape).collect(Collectors.joining(",")) + "\r\n";
    }

    private static String escape(String value) {
        return value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }
}
