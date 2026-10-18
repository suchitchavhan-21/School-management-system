import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Academic Grading Engine.
 * Implements standard GPA calculation (5.0 scale), subject marks validation,
 * percentage computation, and pass/fail logic.
 */
public class GradeCalculator {

    public static final double PASS_MARK = 33.0;
    public static final double MAX_SUBJECT_MARK = 100.0;
    public static final int SUBJECT_COUNT = 5;
    public static final double MAX_TOTAL_MARKS = MAX_SUBJECT_MARK * SUBJECT_COUNT;

    public static class GradeResult {
        public final double totalMarks;
        public final double percentage;
        public final String grade;
        public final double gpa;
        public final String remarks;
        public final boolean passed;

        public GradeResult(double totalMarks, double percentage, String grade, double gpa, String remarks, boolean passed) {
            this.totalMarks = round(totalMarks, 2);
            this.percentage = round(percentage, 2);
            this.grade = grade;
            this.gpa = round(gpa, 2);
            this.remarks = remarks;
            this.passed = passed;
        }

        @Override
        public String toString() {
            return String.format("Total: %.1f/%.0f (%.2f%%) | Grade: %s | GPA: %.2f | Status: %s",
                    totalMarks, MAX_TOTAL_MARKS, percentage, grade, gpa, remarks);
        }
    }

    /**
     * Validates that subject marks are within the legal range [0, 100].
     */
    public static void validateMark(String subjectName, double mark) {
        if (Double.isNaN(mark) || mark < 0.0 || mark > MAX_SUBJECT_MARK) {
            throw new IllegalArgumentException(
                String.format("Invalid mark for %s: %.2f. Marks must be between 0 and 100.", subjectName, mark)
            );
        }
    }

    /**
     * Computes the complete grade result for the 5 core academic subjects.
     */
    public static GradeResult calculate(double english, double math, double science, double socialStudies, double ict) {
        validateMark("English", english);
        validateMark("Mathematics", math);
        validateMark("General Science", science);
        validateMark("Social Studies", socialStudies);
        validateMark("ICT", ict);

        double totalMarks = english + math + science + socialStudies + ict;
        double percentage = (totalMarks / MAX_TOTAL_MARKS) * 100.0;

        // Check if student failed in any individual subject
        boolean failedAnySubject = (english < PASS_MARK) || (math < PASS_MARK) || 
                                   (science < PASS_MARK) || (socialStudies < PASS_MARK) || 
                                   (ict < PASS_MARK);

        if (failedAnySubject) {
            StringBuilder failedSubs = new StringBuilder("Failed in: ");
            if (english < PASS_MARK) failedSubs.append("English ");
            if (math < PASS_MARK) failedSubs.append("Math ");
            if (science < PASS_MARK) failedSubs.append("Science ");
            if (socialStudies < PASS_MARK) failedSubs.append("Social ");
            if (ict < PASS_MARK) failedSubs.append("ICT ");
            return new GradeResult(totalMarks, percentage, "F", 0.0, failedSubs.toString().trim(), false);
        }

        // Standard 5.0 scale grading tier
        String grade;
        double gpa;
        String remarks;

        if (percentage >= 80.0) {
            grade = "A+";
            gpa = 5.0;
            remarks = "Outstanding / Honors";
        } else if (percentage >= 70.0) {
            grade = "A";
            gpa = 4.0;
            remarks = "Very Good";
        } else if (percentage >= 60.0) {
            grade = "A-";
            gpa = 3.5;
            remarks = "Good";
        } else if (percentage >= 50.0) {
            grade = "B";
            gpa = 3.0;
            remarks = "Satisfactory";
        } else if (percentage >= 40.0) {
            grade = "C";
            gpa = 2.0;
            remarks = "Pass";
        } else if (percentage >= 33.0) {
            grade = "D";
            gpa = 1.0;
            remarks = "Minimum Pass";
        } else {
            grade = "F";
            gpa = 0.0;
            remarks = "Failed";
        }

        return new GradeResult(totalMarks, percentage, grade, gpa, remarks, true);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
