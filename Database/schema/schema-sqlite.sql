-- ==============================================================================
-- School Management System - SQLite Production DDL Schema
-- Dialect: SQLite 3.x
-- ==============================================================================

-- 1. Table: pass (User Authentication Credentials)
CREATE TABLE IF NOT EXISTS pass (
    Username TEXT NOT NULL PRIMARY KEY,
    Password TEXT NOT NULL
);

-- 2. Table: admin (Administrator Authorization Key / Master PIN)
CREATE TABLE IF NOT EXISTS admin (
    Password TEXT NOT NULL
);

-- 3. Table: student_info (Student Demographics & Academic Profile)
CREATE TABLE IF NOT EXISTS student_info (
    Roll INTEGER PRIMARY KEY,
    First_name TEXT NOT NULL,
    Last_name TEXT NOT NULL,
    Class INTEGER NOT NULL,
    Year INTEGER NOT NULL,
    Birth_date TEXT NOT NULL,
    Mobile_no TEXT NOT NULL,
    Sex TEXT NOT NULL,
    permanent TEXT NOT NULL,
    Present TEXT NOT NULL
);

-- 4. Table: teacher_info (Faculty Demographics & Departmental Profile)
CREATE TABLE IF NOT EXISTS teacher_info (
    Teachers_id INTEGER PRIMARY KEY,
    First_name TEXT NOT NULL,
    Last_name TEXT NOT NULL,
    Position TEXT NOT NULL,
    Birth_day TEXT NOT NULL,
    Mobile_no TEXT NOT NULL,
    Sex TEXT NOT NULL,
    Permanent TEXT NOT NULL,
    Present TEXT NOT NULL
);

-- 5. Table: employee_info (Staff & Administrative Personnel Profile)
CREATE TABLE IF NOT EXISTS employee_info (
    Employee_id INTEGER PRIMARY KEY,
    First_name TEXT NOT NULL,
    Last_name TEXT NOT NULL,
    Position TEXT NOT NULL,
    Birth_day TEXT NOT NULL,
    Mobile_no TEXT NOT NULL,
    Sex TEXT NOT NULL,
    Permanent TEXT NOT NULL,
    Present TEXT NOT NULL
);

-- 6. Table: exam_marks (Academic Examination Marks & Report Card Data)
CREATE TABLE IF NOT EXISTS exam_marks (
    Mark_id INTEGER PRIMARY KEY AUTOINCREMENT,
    Roll INTEGER NOT NULL,
    Student_name TEXT NOT NULL,
    Class INTEGER NOT NULL,
    Exam_term TEXT NOT NULL,
    Exam_year INTEGER NOT NULL,
    English REAL NOT NULL,
    Math REAL NOT NULL,
    Science REAL NOT NULL,
    Social_studies REAL NOT NULL,
    ICT REAL NOT NULL,
    Total_marks REAL NOT NULL,
    Percentage REAL NOT NULL,
    Grade TEXT NOT NULL,
    GPA REAL NOT NULL,
    Remarks TEXT NOT NULL,
    Created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(Roll, Exam_term, Exam_year)
);

-- Index for rapid exam mark lookups by student
CREATE INDEX IF NOT EXISTS idx_exam_marks_roll ON exam_marks(Roll);
CREATE INDEX IF NOT EXISTS idx_exam_marks_class_term ON exam_marks(Class, Exam_term, Exam_year);

-- 7. Table: attendance (Student Daily Attendance Tracking)
CREATE TABLE IF NOT EXISTS attendance (
    Attendance_id INTEGER PRIMARY KEY AUTOINCREMENT,
    Roll INTEGER NOT NULL,
    Student_name TEXT NOT NULL,
    Class INTEGER NOT NULL,
    Attendance_date TEXT NOT NULL,
    Status TEXT NOT NULL,
    Remarks TEXT,
    Created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(Roll, Attendance_date)
);

-- Index for rapid attendance lookups by student and date
CREATE INDEX IF NOT EXISTS idx_attendance_roll ON attendance(Roll);
CREATE INDEX IF NOT EXISTS idx_attendance_class_date ON attendance(Class, Attendance_date);
