import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Student Daily Attendance Tracking.
 * Enforces parameterized SQL queries, unique (Roll, Attendance_date) constraints,
 * and reliable attendance summary analytics.
 */
public class AttendanceDAO {

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
        if (conn == null || record == null) return false;

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

    public static boolean saveOrUpdate(Connection conn, AttendanceRecord record) throws SQLException {
        if (conn == null || record == null) return false;

        AttendanceRecord existing = findByRollAndDate(conn, record.getRoll(), record.getAttendanceDate());
        if (existing != null) {
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
        } else {
            return insert(conn, record);
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
