import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object for Student Daily Attendance Tracking.
 * Enforces parameterized SQL queries, unique (Roll, Attendance_date) constraints,
 * student resolution against authoritative student_info, strict calendar date validation,
 * and reliable attendance summary analytics.
 */
public class AttendanceDAO {

    private static final DateTimeFormatter STRICT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    public static final int MAX_REMARKS_LENGTH = 255;

    public static final Set<String> VALID_STATUSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("Present", "Absent", "Late", "Excused"))
    );

    /**
     * Determines whether an SQLException represents a genuine unique constraint or duplicate-key
     * collision, as opposed to foreign-key, not-null, check constraint, connection failure,
     * missing table, syntax error, or other unrelated operational failures.
     *
     * In particular, SQLState '23000' alone is a generic integrity constraint class in ANSI SQL
     * and MySQL that encompasses NOT NULL and FOREIGN KEY errors; it NEVER establishes a duplicate
     * violation by itself without verified duplicate vendor codes or duplicate message patterns.
     */
    public static boolean isUniqueConstraintViolation(SQLException ex) {
        if (ex == null) return false;

        int vendorCode = ex.getErrorCode();
        String sqlState = ex.getSQLState();
        String rawMsg = ex.getMessage();
        String msg = rawMsg != null ? rawMsg.toLowerCase() : "";

        // 1. Explicitly exclude non-duplicate integrity constraint violations:
        // MySQL vendor codes: 1048 (ER_BAD_NULL_ERROR), 1364 (ER_NO_DEFAULT_FOR_FIELD),
        // 1216/1217/1451/1452 (foreign key parent/child constraints), 3819 (ER_CHECK_CONSTRAINT_VIOLATED)
        if (vendorCode == 1048 || vendorCode == 1364 ||
            vendorCode == 1216 || vendorCode == 1217 ||
            vendorCode == 1451 || vendorCode == 1452 ||
            vendorCode == 3819) {
            return false;
        }

        // Rule out non-duplicate errors by message pattern across SQLite, MySQL, and generic drivers
        if (msg.contains("foreign key") ||
            msg.contains("may not be null") ||
            msg.contains("cannot be null") ||
            msg.contains("not null constraint failed") ||
            msg.contains("check constraint")) {
            return false;
        }

        // 2. MySQL Vendor Error Codes for genuine duplicate keys:
        // 1062 = ER_DUP_ENTRY ("Duplicate entry '%s' for key '%s'")
        // 1022 = ER_DUP_KEY ("Can't write; duplicate key in table '%s'")
        // 1169 = ER_DUP_UNIQUE ("Can't write, because of unique constraint")
        if (vendorCode == 1062 || vendorCode == 1022 || vendorCode == 1169) {
            return true;
        }

        // 3. ANSI SQL-92/99 specific SQLState for unique_violation:
        // Class 23, Subclass 505 (23505) is specifically Unique Violation (PostgreSQL, Derby, H2, DB2).
        // Generic SQLState 23000 alone is intentionally rejected because it encompasses NOT NULL and FK.
        if ("23505".equals(sqlState)) {
            return true;
        }

        // 4. Exact message patterns for SQLite (both bundled 3.7.2 and modern drivers) and generic drivers:
        // SQLite 3.7.2: "columns Roll, Attendance_date are not unique", "column id is not unique"
        // Modern SQLite: "UNIQUE constraint failed", "PRIMARY KEY must be unique"
        // Generic drivers / wrapped connections: "duplicate entry '", "duplicate key"
        if (msg.contains("are not unique") ||
            msg.contains("is not unique") ||
            msg.contains("unique constraint failed") ||
            msg.contains("primary key must be unique") ||
            msg.contains("sqlite_constraint_unique") ||
            msg.contains("sqlite_constraint_primarykey") ||
            msg.contains("duplicate entry '") ||
            msg.contains("duplicate key")) {
            return true;
        }

        return false;
    }

    public static boolean isValidCalendarDate(String dateStr) {
        if (dateStr == null || dateStr.trim().length() != 10) return false;
        try {
            LocalDate.parse(dateStr.trim(), STRICT_DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static String normalizeStatus(String status) {
        if (status == null) return null;
        String trimmed = status.trim();
        for (String valid : VALID_STATUSES) {
            if (valid.equalsIgnoreCase(trimmed)) {
                return valid;
            }
        }
        return null;
    }

    public static class StudentInfoRecord {
        public final int roll;
        public final String fullName;
        public final int studentClass;

        public StudentInfoRecord(int roll, String fullName, int studentClass) {
            this.roll = roll;
            this.fullName = fullName;
            this.studentClass = studentClass;
        }
    }

    public static StudentInfoRecord resolveStudent(Connection conn, int roll) throws SQLException {
        if (conn == null) throw new SQLException("Cannot resolve student: Database connection is null.");
        String sql = "SELECT First_name, Last_name, Class FROM student_info WHERE Roll = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, roll);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String first = rs.getString("First_name");
                    String last = rs.getString("Last_name");
                    String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                    int cls = rs.getInt("Class");
                    return new StudentInfoRecord(roll, fullName, cls);
                }
            }
        }
        return null;
    }

    public static void validateRecord(Connection conn, AttendanceRecord record) throws SQLException, IllegalArgumentException {
        if (record == null) {
            throw new IllegalArgumentException("Attendance record cannot be null.");
        }
        if (record.getRoll() <= 0) {
            throw new IllegalArgumentException("Student Roll must be a positive integer.");
        }

        // 1. Validate Date as real calendar date
        String dateStr = record.getAttendanceDate();
        if (!isValidCalendarDate(dateStr)) {
            throw new IllegalArgumentException("Invalid attendance date: '" + dateStr + "'. Date must be a valid calendar date in YYYY-MM-DD format.");
        }
        record.setAttendanceDate(dateStr.trim());

        // 2. Validate Status
        String normalizedStatus = normalizeStatus(record.getStatus());
        if (normalizedStatus == null) {
            throw new IllegalArgumentException("Invalid attendance status: '" + record.getStatus() + "'. Supported values are: Present, Absent, Late, Excused.");
        }
        record.setStatus(normalizedStatus);

        // 3. Validate Remarks bounds
        String remarks = record.getRemarks();
        if (remarks != null && remarks.length() > MAX_REMARKS_LENGTH) {
            throw new IllegalArgumentException("Remarks exceed maximum allowed length of " + MAX_REMARKS_LENGTH + " characters (submitted length: " + remarks.length() + ").");
        }

        // 4. Resolve against student_info
        StudentInfoRecord student = resolveStudent(conn, record.getRoll());
        if (student == null) {
            throw new IllegalArgumentException("Student Roll " + record.getRoll() + " does not exist in student database (student_info).");
        }

        // 5. Enforce Class match
        if (record.getStudentClass() != student.studentClass) {
            throw new IllegalArgumentException("Submitted class (" + record.getStudentClass() + ") does not match database record (" + student.studentClass + ") for Roll " + record.getRoll() + ".");
        }

        // 6. Check if submitted name disagrees with authoritative database record
        String submittedName = record.getStudentName();
        if (submittedName != null && !submittedName.trim().isEmpty()) {
            String cleanSubmitted = submittedName.trim().replaceAll("\\s+", " ");
            String cleanDbName = student.fullName.replaceAll("\\s+", " ");
            if (!cleanSubmitted.equalsIgnoreCase(cleanDbName)) {
                throw new IllegalArgumentException("Submitted student name ('" + submittedName + "') does not match database record ('" + student.fullName + "') for Roll " + record.getRoll() + ".");
            }
        }

        // Treat database student record as authoritative for student name and class
        record.setStudentName(student.fullName);
        record.setStudentClass(student.studentClass);
    }

    public static class AttendanceRecord {
        private int attendanceId;
        private int roll;
        private String studentName;
        private int studentClass;
        private String attendanceDate; // YYYY-MM-DD
        private String status;         // Present, Absent, Late, Excused
        private String remarks;

        public AttendanceRecord() {}

        public AttendanceRecord(int roll, String studentName, int studentClass,
                                String attendanceDate, String status, String remarks) {
            this.roll = roll;
            this.studentName = studentName;
            this.studentClass = studentClass;
            this.attendanceDate = attendanceDate;
            this.status = status;
            this.remarks = remarks;
        }

        public AttendanceRecord(int attendanceId, int roll, String studentName, int studentClass,
                                String attendanceDate, String status, String remarks) {
            this(roll, studentName, studentClass, attendanceDate, status, remarks);
            this.attendanceId = attendanceId;
        }

        public int getAttendanceId() { return attendanceId; }
        public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }

        public int getRoll() { return roll; }
        public void setRoll(int roll) { this.roll = roll; }

        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }

        public int getStudentClass() { return studentClass; }
        public void setStudentClass(int studentClass) { this.studentClass = studentClass; }

        public String getAttendanceDate() { return attendanceDate; }
        public void setAttendanceDate(String attendanceDate) { this.attendanceDate = attendanceDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    public static class AttendanceSummary {
        private final int roll;
        private final int totalDays;
        private final int presentDays;
        private final int absentDays;
        private final int lateDays;
        private final int excusedDays;
        private final double attendancePercentage;

        public AttendanceSummary(int roll, int totalDays, int presentDays,
                                 int absentDays, int lateDays, int excusedDays,
                                 double attendancePercentage) {
            this.roll = roll;
            this.totalDays = totalDays;
            this.presentDays = presentDays;
            this.absentDays = absentDays;
            this.lateDays = lateDays;
            this.excusedDays = excusedDays;
            this.attendancePercentage = attendancePercentage;
        }

        public int getRoll() { return roll; }
        public int getTotalDays() { return totalDays; }
        public int getPresentDays() { return presentDays; }
        public int getAbsentDays() { return absentDays; }
        public int getLateDays() { return lateDays; }
        public int getExcusedDays() { return excusedDays; }
        public double getAttendancePercentage() { return attendancePercentage; }
    }

    public static boolean insert(Connection conn, AttendanceRecord record) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        validateRecord(conn, record);

        String sql = "INSERT INTO attendance (Roll, Student_name, Class, Attendance_date, Status, Remarks) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, record.getRoll());
            pst.setString(2, record.getStudentName());
            pst.setInt(3, record.getStudentClass());
            pst.setString(4, record.getAttendanceDate());
            pst.setString(5, record.getStatus());
            pst.setString(6, record.getRemarks() != null ? record.getRemarks() : "");
            return pst.executeUpdate() > 0;
        }
    }

    public static boolean update(Connection conn, AttendanceRecord record) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        validateRecord(conn, record);

        String sql = "UPDATE attendance SET Student_name = ?, Class = ?, Status = ?, Remarks = ? " +
                     "WHERE Roll = ? AND Attendance_date = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, record.getStudentName());
            pst.setInt(2, record.getStudentClass());
            pst.setString(3, record.getStatus());
            pst.setString(4, record.getRemarks() != null ? record.getRemarks() : "");
            pst.setInt(5, record.getRoll());
            pst.setString(6, record.getAttendanceDate());
            return pst.executeUpdate() > 0;
        }
    }

    public static boolean saveOrUpdate(Connection conn, AttendanceRecord record) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        validateRecord(conn, record);

        AttendanceRecord existing = findByRollAndDate(conn, record.getRoll(), record.getAttendanceDate());
        if (existing != null) {
            return update(conn, record);
        } else {
            try {
                return insert(conn, record);
            } catch (SQLException e) {
                // Narrowly scoped handling: only fall back to update if this is a genuine duplicate-key conflict
                if (isUniqueConstraintViolation(e)) {
                    try {
                        return update(conn, record);
                    } catch (SQLException updateEx) {
                        updateEx.addSuppressed(e);
                        throw updateEx;
                    }
                }
                // Do not misclassify or swallow unrelated SQL failures (syntax errors, missing tables, connection drops)
                throw e;
            }
        }
    }

    public static boolean delete(Connection conn, int roll, String attendanceDate) throws SQLException {
        if (conn == null || attendanceDate == null) return false;

        String sql = "DELETE FROM attendance WHERE Roll = ? AND Attendance_date = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, roll);
            pst.setString(2, attendanceDate.trim());
            return pst.executeUpdate() > 0;
        }
    }

    public static AttendanceRecord findByRollAndDate(Connection conn, int roll, String attendanceDate) throws SQLException {
        if (conn == null || attendanceDate == null) return null;

        String sql = "SELECT * FROM attendance WHERE Roll = ? AND Attendance_date = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, roll);
            pst.setString(2, attendanceDate.trim());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public static List<AttendanceRecord> findByRoll(Connection conn, int roll) throws SQLException {
        List<AttendanceRecord> list = new ArrayList<>();
        if (conn == null) return list;

        String sql = "SELECT * FROM attendance WHERE Roll = ? ORDER BY Attendance_date DESC";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, roll);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public static List<AttendanceRecord> findByClassAndDate(Connection conn, int studentClass, String attendanceDate) throws SQLException {
        List<AttendanceRecord> list = new ArrayList<>();
        if (conn == null) return list;

        String sql = "SELECT * FROM attendance WHERE Class = ? AND Attendance_date = ? ORDER BY Roll ASC";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentClass);
            pst.setString(2, attendanceDate.trim());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public static List<AttendanceRecord> findAll(Connection conn) throws SQLException {
        List<AttendanceRecord> list = new ArrayList<>();
        if (conn == null) return list;

        String sql = "SELECT * FROM attendance ORDER BY Attendance_date DESC, Roll ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public static AttendanceSummary calculateSummary(Connection conn, int roll) throws SQLException {
        if (conn == null) {
            return new AttendanceSummary(roll, 0, 0, 0, 0, 0, 0.0);
        }

        String sql = "SELECT Status, COUNT(*) as cnt FROM attendance WHERE Roll = ? GROUP BY Status";
        int present = 0;
        int absent = 0;
        int late = 0;
        int excused = 0;
        int total = 0;

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, roll);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("Status");
                    int count = rs.getInt("cnt");
                    total += count;
                    if (status != null) {
                        switch (status.trim().toLowerCase()) {
                            case "present":
                                present = count;
                                break;
                            case "absent":
                                absent = count;
                                break;
                            case "late":
                                late = count;
                                break;
                            case "excused":
                                excused = count;
                                break;
                        }
                    }
                }
            }
        }

        double percentage = 0.0;
        if (total > 0) {
            // Standard academic formula: (Present + Late) / Total * 100
            percentage = Math.round(((double)(present + late) / total * 100.0) * 100.0) / 100.0;
        }

        return new AttendanceSummary(roll, total, present, absent, late, excused, percentage);
    }

    private static AttendanceRecord mapResultSet(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
                rs.getInt("Attendance_id"),
                rs.getInt("Roll"),
                rs.getString("Student_name"),
                rs.getInt("Class"),
                rs.getString("Attendance_date"),
                rs.getString("Status"),
                rs.getString("Remarks")
        );
    }
}
