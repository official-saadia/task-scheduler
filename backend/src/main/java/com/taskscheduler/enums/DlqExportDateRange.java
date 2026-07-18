package com.taskscheduler.enums;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Preset date ranges for the DLQ export. Each constant knows how to resolve
 * itself to a concrete {@code [from, to]} window using {@code java.time},
 * relative to "now" when the export is requested.
 */
public enum DlqExportDateRange {
    TODAY {
        @Override
        public LocalDateTime from(LocalDate today) {
            return today.atStartOfDay();
        }
    },
    YESTERDAY {
        @Override
        public LocalDateTime from(LocalDate today) {
            return today.minusDays(1).atStartOfDay();
        }

        @Override
        public LocalDateTime to(LocalDate today) {
            return today.atStartOfDay().minusNanos(1);
        }
    },
    PAST_7_DAYS {
        @Override
        public LocalDateTime from(LocalDate today) {
            return today.minusDays(6).atStartOfDay();
        }
    },
    PAST_30_DAYS {
        @Override
        public LocalDateTime from(LocalDate today) {
            return today.minusDays(29).atStartOfDay();
        }
    };

    /** Inclusive start of the window. */
    public abstract LocalDateTime from(LocalDate today);

    /** Inclusive end of the window. Defaults to the end of today. */
    public LocalDateTime to(LocalDate today) {
        return LocalDateTime.of(today, LocalTime.MAX);
    }
}
