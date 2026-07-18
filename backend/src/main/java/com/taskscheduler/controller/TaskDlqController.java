package com.taskscheduler.controller;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqExportDateRange;
import com.taskscheduler.enums.DlqStatus;
import com.taskscheduler.service.DlqExportService;
import com.taskscheduler.service.TaskDlqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/task-dlq")
@RequiredArgsConstructor
@Tag(name = "Dead Letter Queue", description = "APIs for reviewing and managing failed tasks in the Dead Letter Queue")
public class TaskDlqController {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TaskDlqService taskDlqService;
    private final DlqExportService dlqExportService;

    @Operation(summary = "Get all DLQ entries with pagination")
    @ApiResponse(responseCode = "200", description = "DLQ entries retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<TaskDlq>> getAllDlqEntries(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskDlqService.getAllDlqEntries(page, size));
    }

    @Operation(summary = "Get DLQ entry by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entry found"),
            @ApiResponse(responseCode = "404", description = "DLQ entry not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskDlq> getDlqEntryById(
            @Parameter(description = "DLQ entry ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskDlqService.getDlqEntryById(id));
    }

    @Operation(summary = "Get DLQ entries by status with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entries retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<TaskDlq>> getDlqEntriesByStatus(
            @Parameter(description = "DLQ status", required = true) @PathVariable DlqStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskDlqService.getDlqEntriesByStatus(status, page, size));
    }

    @Operation(summary = "Update DLQ entry status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entry status updated successfully"),
            @ApiResponse(responseCode = "404", description = "DLQ entry not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDlq> updateDlqStatus(
            @Parameter(description = "DLQ entry ID", required = true) @PathVariable Long id,
            @Parameter(description = "New status", required = true) @RequestParam DlqStatus status) {
        return ResponseEntity.ok(taskDlqService.updateDlqStatus(id, status));
    }

    @Operation(
            summary = "Export DLQ entries as an XLSX file",
            description = "Generates an Excel workbook of DLQ entries for either a preset date range "
                    + "or an explicit custom range. Supply exactly one of: 'dateRange' (preset), or both "
                    + "'from' and 'to' (custom, ISO yyyy-MM-dd, both inclusive). Optionally restrict to a "
                    + "single 'status'; omit it to include every status in the window. "
                    + "Native export, computed in-process from Task Scheduler's own DLQ data — no external "
                    + "script required. Can be called manually (download button) or from a REPORT_GENERATION "
                    + "task's command (e.g. curl) to feed a scheduled email attachment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "XLSX file generated successfully"),
            @ApiResponse(responseCode = "400", description = "Neither or both range modes supplied, or 'to' precedes 'from'")
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDlq(
            @Parameter(description = "Preset date range. Omit when supplying 'from'/'to'.")
            @RequestParam(required = false) DlqExportDateRange dateRange,

            @Parameter(description = "Custom range start, inclusive (yyyy-MM-dd). Requires 'to'.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Custom range end, inclusive (yyyy-MM-dd). Requires 'from'.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Restrict the export to a single status. Omit to include all statuses.")
            @RequestParam(required = false) DlqStatus status) {

        boolean hasPreset = dateRange != null;
        boolean hasCustom = from != null || to != null;

        if (hasPreset == hasCustom) {
            throw new IllegalArgumentException(
                    "Supply either 'dateRange' for a preset window, or both 'from' and 'to' for a custom "
                            + "range — not both, and not neither.");
        }

        byte[] workbook;
        String rangeLabel;

        if (hasPreset) {
            workbook = dlqExportService.exportToXlsx(dateRange, status);
            rangeLabel = dateRange.name().toLowerCase() + "_" + LocalDate.now().format(FILE_DATE);
        } else {
            if (from == null || to == null) {
                throw new IllegalArgumentException(
                        "A custom range needs both 'from' and 'to'; only '"
                                + (from != null ? "from" : "to") + "' was supplied.");
            }
            if (to.isBefore(from)) {
                throw new IllegalArgumentException(
                        "'to' (" + to + ") must not be before 'from' (" + from + ").");
            }
            workbook = dlqExportService.exportToXlsx(from.atStartOfDay(), to.atTime(LocalTime.MAX), status);
            rangeLabel = "custom_" + from.format(FILE_DATE) + "_" + to.format(FILE_DATE);
        }

        String statusLabel = (status == null) ? "" : status.name().toLowerCase() + "_";
        String filename = "dlq_report_" + statusLabel + rangeLabel + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(workbook);
    }
}
