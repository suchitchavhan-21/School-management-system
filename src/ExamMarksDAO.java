import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Academic Examination Marks.
 * Implements 100% parameterized queries, proper resource management,
 * and robust error handling.
 */
public class ExamMarksDAO {

    public static class ExamMark {
        public int markId;
        public int roll;
        public String studentName;
        public int className;
        public String examTerm;
        public int examYear;
        public double english;
        public double math;
        public double science;
        public double socialStudies;
        public double ict;
        public double totalMarks;
        public double percentage;
        public String grade;
        public double gpa;
        public String remarks;

        public ExamMark() {}

        public ExamMark(int roll, String studentName, int className, String examTerm, int examYear,
                        double english, double math, double science, double socialStudies, double ict,
                        GradeCalculator.GradeResult calcResult) {
            this.roll = roll;
            this.studentName = studentName;
            this.className = className;
            this.examTerm = examTerm;
            this.examYear = examYear;
            this.english = english;
            this.math = math;
            this.science = science;
            this.socialStudies = socialStudies;
            this.ict = ict;
            this.totalMarks = calcResult.totalMarks;
            this.percentage = calcResult.percentage;
            this.grade = calcResult.grade;
            this.gpa = calcResult.gpa;
            this.remarks = calcResult.remarks;
        }
    }

    /**
     * Inserts a new exam mark record using parameterized queries.
     */
    public static boolean insert(Connection conn, ExamMark mark) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        String sql = "INSERT INTO exam_marks (" +
                "Roll, Student_name, Class, Exam_term, Exam_year, " +
                "English, Math, Science, Social_studies, ICT, " +
                "Total_marks, Percentage, Grade, GPA, Remarks" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, mark.roll);
            pst.setString(2, mark.studentName);
            pst.setInt(3, mark.className);
            pst.setString(4, mark.examTerm);
            pst.setInt(5, mark.examYear);
            pst.setDouble(6, mark.english);
            pst.setDouble(7, mark.math);
            pst.setDouble(8, mark.science);
            pst.setDouble(9, mark.socialStudies);
            pst.setDouble(10, mark.ict);
            pst.setDouble(11, mark.totalMarks);
            pst.setDouble(12, mark.percentage);
            pst.setString(13, mark.grade);
            pst.setDouble(14, mark.gpa);
            pst.setString(15, mark.remarks);

            int affected = pst.executeUpdate();
            return affected > 0;
        }
    }

    /**
     * Updates an existing exam mark record by Roll, Term, and Year.
     */
    public static boolean update(Connection conn, ExamMark mark) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        String sql = "UPDATE exam_marks SET " +
                "Student_name = ?, Class = ?, " +
                "English = ?, Math = ?, Science = ?, Social_studies = ?, ICT = ?, " +
                "Total_marks = ?, Percentage = ?, Grade = ?, GPA = ?, Remarks = ? " +
                "WHERE Roll = ? AND Exam_term = ? AND Exam_year = ?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, mark.studentName);
            pst.setInt(2, mark.className);
            pst.setDouble(3, mark.english);
            pst.setDouble(4, mark.math);
            pst.setDouble(5, mark.science);
            pst.setDouble(6, mark.socialStudies);
            pst.setDouble(7, mark.ict);
            pst.setDouble(8, mark.totalMarks);
            pst.setDouble(9, mark.percentage);
            pst.setString(10, mark.grade);
            pst.setDouble(11, mark.gpa);
            pst.setString(12, mark.remarks);
            pst.setInt(13, mark.roll);
            pst.setString(14, mark.examTerm);
            pst.setInt(15, mark.examYear);

            int affected = pst.executeUpdate();
            return affected > 0;
        }
    }

    /**
     * Saves or updates a mark record based on existence check.
     * Narrowly handles concurrent insert races on unique key (Roll, Exam_term, Exam_year).
     */
    public static boolean saveOrUpdate(Connection conn, ExamMark mark) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        ExamMark existing = findByRollTermYear(conn, mark.roll, mark.examTerm, mark.examYear);
        if (existing != null) {
            return update(conn, mark);
        } else {
            try {
                return insert(conn, mark);
            } catch (SQLException e) {
                if (AttendanceDAO.isUniqueConstraintViolation(e)) {
                    try {
                        return update(conn, mark);
                    } catch (SQLException updateEx) {
                        updateEx.addSuppressed(e);
                        throw updateEx;
                    }
                }
                throw e;
            }
        }
    }

    /**
     * Deletes an exam mark record.
     */
    public static boolean delete(Connection conn, int roll, String examTerm, int examYear) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        String sql = "DELETE FROM exam_marks WHERE Roll = ? AND Exam_term = ? AND Exam_year = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, roll);
            pst.setString(2, examTerm);
            pst.setInt(3, examYear);
            return pst.executeUpdate() > 0;
        }
    }

    /**
     * Finds a single exam mark by Roll, Term, and Year.
     */
    public static ExamMark findByRollTermYear(Connection conn, int roll, String examTerm, int examYear) throws SQLException {
        if (conn == null) throw new SQLException("Database connection is null.");
        String sql = "SELECT * FROM exam_marks WHERE Roll = ? AND Exam_term = ? AND Exam_year = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, roll);
            pst.setString(2, examTerm);
            pst.setInt(3, examYear);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Searches for marks by Roll or Student Name.
     */
    public static List<ExamMark> search(Connection conn, String query) throws SQLException {
        List<ExamMark> results = new ArrayList<>();
        String sql = "SELECT * FROM exam_marks WHERE Roll = ? OR Student_name LIKE ? ORDER BY Exam_year DESC, Exam_term ASC";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            int roll = -1;
            try {
                roll = Integer.parseInt(query.trim());
            } catch (NumberFormatException ignored) {}

            pst.setInt(1, roll);
            pst.setString(2, "%" + query.trim() + "%");

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    /**
     * Retrieves all marks ordered by class and roll.
     */
    public static List<ExamMark> findAll(Connection conn) throws SQLException {
        List<ExamMark> results = new ArrayList<>();
        String sql = "SELECT * FROM exam_marks ORDER BY Class ASC, Roll ASC, Exam_year DESC";
        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private static ExamMark mapRow(ResultSet rs) throws SQLException {
        ExamMark mark = new ExamMark();
        mark.markId = rs.getInt("Mark_id");
        mark.roll = rs.getInt("Roll");
        mark.studentName = rs.getString("Student_name");
        mark.className = rs.getInt("Class");
        mark.examTerm = rs.getString("Exam_term");
        mark.examYear = rs.getInt("Exam_year");
        mark.english = rs.getDouble("English");
        mark.math = rs.getDouble("Math");
        mark.science = rs.getDouble("Science");
        mark.socialStudies = rs.getDouble("Social_studies");
        mark.ict = rs.getDouble("ICT");
        mark.totalMarks = rs.getDouble("Total_marks");
        mark.percentage = rs.getDouble("Percentage");
        mark.grade = rs.getString("Grade");
        mark.gpa = rs.getDouble("GPA");
        mark.remarks = rs.getString("Remarks");
        return mark;
    }
}
