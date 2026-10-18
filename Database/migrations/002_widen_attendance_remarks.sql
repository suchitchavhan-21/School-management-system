-- ==============================================================================
-- Migration: 002_widen_attendance_remarks.sql
-- Module: Student Daily Attendance Management
-- Target Dialect: MySQL 5.7+ / 8.0+
-- Description:
--   Widens the 'Remarks' column in the 'attendance' table from VARCHAR(100)
--   to VARCHAR(255) to align database storage with application-level validation
--   (AttendanceDAO.MAX_REMARKS_LENGTH = 255) and prevent truncation errors
--   under MySQL strict mode (STRICT_TRANS_TABLES).
-- Safe Execution:
--   - Non-destructive: In-place column widening without data truncation.
--   - Nullable default preserved.
-- ==============================================================================

ALTER TABLE `attendance` MODIFY COLUMN `Remarks` VARCHAR(255) DEFAULT NULL;
