-- ==============================================================================
-- School Management System - MySQL Production DDL Schema
-- Dialect: MySQL 5.7+ / 8.0+ / MariaDB 10.3+
-- Engine: InnoDB | Character Set: utf8mb4 | Collation: utf8mb4_unicode_ci
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Table: pass (User Authentication Credentials)
CREATE TABLE IF NOT EXISTS `pass` (
    `Username` VARCHAR(50) NOT NULL,
    `Password` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`Username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Table: admin (Administrator Authorization Key / Master PIN)
CREATE TABLE IF NOT EXISTS `admin` (
    `Password` VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Table: student_info (Student Demographics & Academic Profile)
CREATE TABLE IF NOT EXISTS `student_info` (
    `Roll` INT NOT NULL,
    `First_name` VARCHAR(50) NOT NULL,
    `Last_name` VARCHAR(50) NOT NULL,
    `Class` INT NOT NULL,
    `Year` INT NOT NULL,
    `Birth_date` VARCHAR(50) NOT NULL,
    `Mobile_no` VARCHAR(50) NOT NULL,
    `Sex` VARCHAR(20) NOT NULL,
    `permanent` VARCHAR(100) NOT NULL,
    `Present` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`Roll`),
    INDEX `idx_student_class` (`Class`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Table: teacher_info (Faculty Demographics & Departmental Profile)
CREATE TABLE IF NOT EXISTS `teacher_info` (
    `Teachers_id` INT NOT NULL,
    `First_name` VARCHAR(50) NOT NULL,
    `Last_name` VARCHAR(50) NOT NULL,
    `Position` VARCHAR(50) NOT NULL,
    `Birth_day` VARCHAR(50) NOT NULL,
    `Mobile_no` VARCHAR(50) NOT NULL,
    `Sex` VARCHAR(20) NOT NULL,
    `Permanent` VARCHAR(100) NOT NULL,
    `Present` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`Teachers_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Table: employee_info (Staff & Administrative Personnel Profile)
CREATE TABLE IF NOT EXISTS `employee_info` (
    `Employee_id` INT NOT NULL,
    `First_name` VARCHAR(50) NOT NULL,
    `Last_name` VARCHAR(50) NOT NULL,
    `Position` VARCHAR(50) NOT NULL,
    `Birth_day` VARCHAR(50) NOT NULL,
    `Mobile_no` VARCHAR(50) NOT NULL,
    `Sex` VARCHAR(20) NOT NULL,
    `Permanent` VARCHAR(100) NOT NULL,
    `Present` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`Employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Table: exam_marks (Academic Examination Marks & Report Card Data)
CREATE TABLE IF NOT EXISTS `exam_marks` (
    `Mark_id` INT NOT NULL AUTO_INCREMENT,
    `Roll` INT NOT NULL,
    `Student_name` VARCHAR(100) NOT NULL,
    `Class` INT NOT NULL,
    `Exam_term` VARCHAR(50) NOT NULL,
    `Exam_year` INT NOT NULL,
    `English` DOUBLE NOT NULL,
    `Math` DOUBLE NOT NULL,
    `Science` DOUBLE NOT NULL,
    `Social_studies` DOUBLE NOT NULL,
    `ICT` DOUBLE NOT NULL,
    `Total_marks` DOUBLE NOT NULL,
    `Percentage` DOUBLE NOT NULL,
    `Grade` VARCHAR(10) NOT NULL,
    `GPA` DOUBLE NOT NULL,
    `Remarks` VARCHAR(50) NOT NULL,
    `Created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`Mark_id`),
    UNIQUE KEY `uq_exam_student_term_year` (`Roll`, `Exam_term`, `Exam_year`),
    INDEX `idx_exam_roll` (`Roll`),
    INDEX `idx_exam_class_term` (`Class`, `Exam_term`, `Exam_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Table: attendance (Student Daily Attendance Tracking)
CREATE TABLE IF NOT EXISTS `attendance` (
    `Attendance_id` INT NOT NULL AUTO_INCREMENT,
    `Roll` INT NOT NULL,
    `Student_name` VARCHAR(100) NOT NULL,
    `Class` INT NOT NULL,
    `Attendance_date` VARCHAR(20) NOT NULL,
    `Status` VARCHAR(20) NOT NULL,
    `Remarks` VARCHAR(100) DEFAULT NULL,
    `Created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`Attendance_id`),
    UNIQUE KEY `uq_attendance_student_date` (`Roll`, `Attendance_date`),
    INDEX `idx_attendance_roll` (`Roll`),
    INDEX `idx_attendance_class_date` (`Class`, `Attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
