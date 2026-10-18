/**
 * Master Automated Test Suite Runner.
 * Executes all component, integration, and security test suites.
 * Exits with status 0 ONLY if all tests execute and pass with zero failures and zero skips.
 */
public class SystemTestSuite {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   SCHOOL MANAGEMENT SYSTEM - ZERO-TRUST VERIFICATION & QUALITY TEST HARNESS    ");
        System.out.println("================================================================================\n");

        long startTime = System.currentTimeMillis();

        // 1. Grade Calculation Engine Suite
        GradeCalculatorTest calcTest = new GradeCalculatorTest();
        boolean calcPassed = calcTest.runAllTests();

        // 2. Examination Marks DAO & Persistence Suite (Isolated DB)
        ExamMarksDAOTest daoTest = new ExamMarksDAOTest();
        boolean daoPassed = daoTest.runAllTests();

        // 3. Student Attendance DAO & Tracking Suite (Isolated DB)
        AttendanceDAOTest attendanceTest = new AttendanceDAOTest();
        boolean attendancePassed = attendanceTest.runAllTests();

        // 4. Security, Ethical Hacking & Resilience Suite (Isolated DB)
        SecurityAndResilienceTest secTest = new SecurityAndResilienceTest();
        boolean secPassed = secTest.runAllTests();

        long duration = System.currentTimeMillis() - startTime;

        int totalRun = calcTest.getTestsRun() + daoTest.getTestsRun() + attendanceTest.getTestsRun() + secTest.getTestsRun();
        int totalPassed = calcTest.getTestsPassed() + daoTest.getTestsPassed() + attendanceTest.getTestsPassed() + secTest.getTestsPassed();
        int totalFailed = calcTest.getTestsFailed() + daoTest.getTestsFailed() + attendanceTest.getTestsFailed() + secTest.getTestsFailed();
        int totalSkipped = calcTest.getTestsSkipped() + daoTest.getTestsSkipped() + attendanceTest.getTestsSkipped() + secTest.getTestsSkipped();

        boolean allPassed = calcPassed && daoPassed && attendancePassed && secPassed && (totalFailed == 0) && (totalSkipped == 0) && (totalRun > 0);

        System.out.println("================================================================================");
        System.out.println("                     COMPREHENSIVE TEST SUITE SUMMARY                           ");
        System.out.println("================================================================================");
        System.out.printf("  GradeCalculatorTest:       %3d Passed, %2d Failed, %2d Skipped / %3d Total\n",
                calcTest.getTestsPassed(), calcTest.getTestsFailed(), calcTest.getTestsSkipped(), calcTest.getTestsRun());
        System.out.printf("  ExamMarksDAOTest:          %3d Passed, %2d Failed, %2d Skipped / %3d Total\n",
                daoTest.getTestsPassed(), daoTest.getTestsFailed(), daoTest.getTestsSkipped(), daoTest.getTestsRun());
        System.out.printf("  AttendanceDAOTest:         %3d Passed, %2d Failed, %2d Skipped / %3d Total\n",
                attendanceTest.getTestsPassed(), attendanceTest.getTestsFailed(), attendanceTest.getTestsSkipped(), attendanceTest.getTestsRun());
        System.out.printf("  SecurityAndResilienceTest: %3d Passed, %2d Failed, %2d Skipped / %3d Total\n",
                secTest.getTestsPassed(), secTest.getTestsFailed(), secTest.getTestsSkipped(), secTest.getTestsRun());
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("  TOTAL SUITE RESULTS:       %3d Passed, %2d Failed, %2d Skipped / %3d Total\n",
                totalPassed, totalFailed, totalSkipped, totalRun);
        System.out.printf("  EXECUTION TIME:            %d ms\n", duration);
        System.out.println("================================================================================");

        if (allPassed) {
            System.out.println(">>> VERDICT: 100% GREEN - ALL TEST SUITES PASSED RIGOROUS ZERO-TRUST AUDIT. <<<");
            System.out.println("================================================================================");
            System.exit(0);
        } else {
            System.err.println(">>> VERDICT: FAILED - ONE OR MORE TEST SUITES RECORDED FAILURES OR SKIPS! <<<");
            System.out.println("================================================================================");
            System.exit(1);
        }
    }
}
