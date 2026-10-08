package com.kubee.pos.reporting.application.export;

import java.util.List;

/** Turns report tables into a downloadable file. One implementation per {@link ExportFormat}. */
public interface ReportFileWriter {

    ExportFormat format();

    byte[] write(List<ReportTable> tables);
}
