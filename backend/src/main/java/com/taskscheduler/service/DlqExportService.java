package com.taskscheduler.service;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqExportDateRange;
import com.taskscheduler.enums.DlqStatus;
import com.taskscheduler.repository.TaskDlqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds an XLSX export of Dead Letter Queue entries for a given date range.
 *
 * <p>This is a native feature, not a {@code REPORT_GENERATION} task: the DLQ
 * is Task Scheduler's own data, so it's generated in-process rather than via
 * an external command. It's exposed two ways:</p>
 * <ul>
 *   <li>Manually, via the download button on the DLQ page</li>
 *   <li>On a schedule, by pointing a {@code REPORT_GENERATION} task's command
 *       at the {@code GET /api/v1/task-dlq/export} endpoint (e.g. with curl),
 *       writing the response to a file that an EMAIL_NOTIFICATION task's
 *       attachmentPath then picks up</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {
            "DLQ ID", "Task Name", "Task Type", "Status", "Retry Count", "Failure Reason", "Failed At"
    };

    private final TaskDlqRepository taskDlqRepository;

    /**
     * Generates an XLSX workbook of DLQ entries within the given preset range,
     * relative to today, across all statuses.
     *
     * @param dateRange the preset window to export
     * @return the workbook contents as bytes, ready to stream as a file download
     */
    @Transactional(readOnly = true)
    public byte[] exportToXlsx(DlqExportDateRange dateRange) {
        return exportToXlsx(dateRange, null);
    }

    /**
     * Generates an XLSX workbook of DLQ entries within the given preset range,
     * relative to today, optionally restricted to a single status.
     *
     * @param dateRange the preset window to export
     * @param status    the status to filter by, or {@code null} for all statuses
     * @return the workbook contents as bytes, ready to stream as a file download
     */
    @Transactional(readOnly = true)
    public byte[] exportToXlsx(DlqExportDateRange dateRange, DlqStatus status) {
        LocalDate today = LocalDate.now();
        return exportToXlsx(dateRange.from(today), dateRange.to(today), status);
    }

    /**
     * Generates an XLSX workbook of DLQ entries within an explicit window,
     * across all statuses.
     *
     * @param from inclusive start of the window
     * @param to   inclusive end of the window
     * @return the workbook contents as bytes
     */
    @Transactional(readOnly = true)
    public byte[] exportToXlsx(LocalDateTime from, LocalDateTime to) {
        return exportToXlsx(from, to, null);
    }

    /**
     * Generates an XLSX workbook of DLQ entries within an explicit window,
     * optionally restricted to a single status.
     *
     * @param from   inclusive start of the window
     * @param to     inclusive end of the window
     * @param status the status to filter by, or {@code null} for all statuses
     * @return the workbook contents as bytes
     */
    @Transactional(readOnly = true)
    public byte[] exportToXlsx(LocalDateTime from, LocalDateTime to, DlqStatus status) {
        List<TaskDlq> entries = (status == null)
                ? taskDlqRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to)
                : taskDlqRepository.findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(status, from, to);

        log.info("DlqExportService: Exporting {} DLQ entries for range {} to {} | status: {}",
                entries.size(), from, to, status == null ? "ALL" : status);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("DLQ Report");
            CellStyle headerStyle = buildHeaderStyle(workbook);

            writeHeaderRow(sheet, headerStyle);
            writeDataRows(sheet, entries);
            autoSizeColumns(sheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate DLQ export workbook", ex);
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeDataRows(Sheet sheet, List<TaskDlq> entries) {
        int rowIndex = 1;
        for (TaskDlq entry : entries) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getId());
            row.createCell(1).setCellValue(entry.getTask() != null ? entry.getTask().getName() : "");
            row.createCell(2).setCellValue(entry.getTask() != null ? entry.getTask().getType().name() : "");
            row.createCell(3).setCellValue(entry.getStatus().name());
            row.createCell(4).setCellValue(entry.getTaskExecution() != null
                    ? entry.getTaskExecution().getRetryCount() : 0);
            row.createCell(5).setCellValue(entry.getFailureReason());
            row.createCell(6).setCellValue(entry.getCreatedAt().format(TIMESTAMP_FORMAT));
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
