/**
 * Comprehensive Unit Tests for GradeCalculator.
 */
public class GradeCalculatorTest {

    private int testsRun = 0;
    private int testsPassed = 0;

    public static void main(String[] args) {
        GradeCalculatorTest test = new GradeCalculatorTest();
        test.runAllTests();
    }

    public boolean runAllTests() {
        System.out.println("==================================================");
        System.out.println("Running GradeCalculatorTest Suite...");
        System.out.println("==================================================");

        testPerfectScore();
        testGradeBoundaries();
        testSingleSubjectFailureRule();
        testInvalidMarksValidation();
        testRoundingAndPrecision();

        System.out.printf("GradeCalculatorTest Result: %d/%d tests passed.\n\n", testsPassed, testsRun);
        return testsRun == testsPassed;
    }

    private void assertTrue(String message, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
        }
    }

    private void assertEquals(String message, Object expected, Object actual) {
        testsRun++;
        if (expected == null && actual == null) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else if (expected != null && expected.equals(actual)) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.printf("  [FAIL] %s - Expected: %s, Got: %s\n", message, expected, actual);
        }
    }

    private void assertEquals(String message, double expected, double actual, double delta) {
        testsRun++;
        if (Math.abs(expected - actual) <= delta) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.printf("  [FAIL] %s - Expected: %.4f, Got: %.4f\n", message, expected, actual);
        }
    }

    private void testPerfectScore() {
        GradeCalculator.GradeResult res = GradeCalculator.calculate(100, 100, 100, 100, 100);
        assertEquals("Perfect score total marks", 500.0, res.totalMarks, 0.001);
        assertEquals("Perfect score percentage", 100.0, res.percentage, 0.001);
        assertEquals("Perfect score grade", "A+", res.grade);
        assertEquals("Perfect score GPA", 5.0, res.gpa, 0.001);
        assertTrue("Perfect score is passed", res.passed);
    }

    private void testGradeBoundaries() {
        // 80.0% -> A+, GPA 5.0
        GradeCalculator.GradeResult res80 = GradeCalculator.calculate(80, 80, 80, 80, 80);
        assertEquals("80% grade is A+", "A+", res80.grade);
        assertEquals("80% GPA is 5.0", 5.0, res80.gpa, 0.001);
        assertTrue("80% is passed", res80.passed);

        // 75.0% -> A, GPA 4.0
        GradeCalculator.GradeResult res75 = GradeCalculator.calculate(75, 75, 75, 75, 75);
        assertEquals("75% grade is A", "A", res75.grade);
        assertEquals("75% GPA is 4.0", 4.0, res75.gpa, 0.001);
        assertTrue("75% is passed", res75.passed);

        // 65.0% -> A-, GPA 3.5
        GradeCalculator.GradeResult res65 = GradeCalculator.calculate(65, 65, 65, 65, 65);
        assertEquals("65% grade is A-", "A-", res65.grade);
        assertEquals("65% GPA is 3.5", 3.5, res65.gpa, 0.001);
        assertTrue("65% is passed", res65.passed);

        // 55.0% -> B, GPA 3.0
        GradeCalculator.GradeResult res55 = GradeCalculator.calculate(55, 55, 55, 55, 55);
        assertEquals("55% grade is B", "B", res55.grade);
        assertEquals("55% GPA is 3.0", 3.0, res55.gpa, 0.001);
        assertTrue("55% is passed", res55.passed);

        // 45.0% -> C, GPA 2.0
        GradeCalculator.GradeResult res45 = GradeCalculator.calculate(45, 45, 45, 45, 45);
        assertEquals("45% grade is C", "C", res45.grade);
        assertEquals("45% GPA is 2.0", 2.0, res45.gpa, 0.001);
        assertTrue("45% is passed", res45.passed);

        // 35.0% -> D, GPA 1.0
        GradeCalculator.GradeResult res35 = GradeCalculator.calculate(35, 35, 35, 35, 35);
        assertEquals("35% grade is D", "D", res35.grade);
        assertEquals("35% GPA is 1.0", 1.0, res35.gpa, 0.001);
        assertTrue("35% is passed", res35.passed);

        // 32.0% -> F, GPA 0.0
        GradeCalculator.GradeResult res32 = GradeCalculator.calculate(32, 32, 32, 32, 32);
        assertEquals("32% grade is F", "F", res32.grade);
        assertEquals("32% GPA is 0.0", 0.0, res32.gpa, 0.001);
        assertTrue("32% is not passed", !res32.passed);
    }

    private void testSingleSubjectFailureRule() {
        // High scores in 4 subjects, but fail in 1 subject (e.g. Science = 25)
        GradeCalculator.GradeResult res = GradeCalculator.calculate(90, 85, 25, 92, 88);
        assertEquals("High average with single subject fail must receive F", "F", res.grade);
        assertEquals("Single subject fail must have 0.0 GPA", 0.0, res.gpa, 0.001);
        assertTrue("Single subject fail must fail overall", !res.passed);
        assertTrue("Remarks must indicate Science failure", res.remarks.contains("Science"));
    }

    private void testInvalidMarksValidation() {
        boolean caughtNegative = false;
        try {
            GradeCalculator.calculate(-5, 80, 80, 80, 80);
        } catch (IllegalArgumentException e) {
            caughtNegative = true;
        }
        assertTrue("Rejects negative marks", caughtNegative);

        boolean caughtOverMax = false;
        try {
            GradeCalculator.calculate(80, 105, 80, 80, 80);
        } catch (IllegalArgumentException e) {
            caughtOverMax = true;
        }
        assertTrue("Rejects marks > 100", caughtOverMax);

        boolean caughtNaN = false;
        try {
            GradeCalculator.calculate(80, 80, Double.NaN, 80, 80);
        } catch (IllegalArgumentException e) {
            caughtNaN = true;
        }
        assertTrue("Rejects NaN marks", caughtNaN);
    }

    private void testRoundingAndPrecision() {
        GradeCalculator.GradeResult res = GradeCalculator.calculate(77.5, 83.2, 69.8, 91.4, 85.5);
        assertEquals("Total marks rounded to 2 places", 407.4, res.totalMarks, 0.01);
        assertEquals("Percentage rounded to 2 places", 81.48, res.percentage, 0.01);
        assertEquals("Grade with 81.48% is A+", "A+", res.grade);
    }
}
