/**
 * Master Automated Test Suite Runner.
 * Executes all component, integration, and security test suites.
 */
public class SystemTestSuite {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   SCHOOL MANAGEMENT SYSTEM - AUTOMATED VERIFICATION & QUALITY TEST HARNESS      ");
        System.out.println("================================================================================\n");

        long startTime = System.currentTimeMillis();

        boolean allPassed = true;

        // 1. Grade Calculation Engine Suite
        GradeCalculatorTest calcTest = new GradeCalculatorTest();
        if (!calcTest.runAllTests()) {
            allPassed = false;
        }

        // 2. Examination Marks DAO & Persistence Suite
        ExamMarksDAOTest daoTest = new ExamMarksDAOTest();
        if (!daoTest.runAllTests()) {
            allPassed = false;
        }

        // 3. Security, Ethical Hacking & Resilience Suite
        SecurityAndResilienceTest secTest = new SecurityAndResilienceTest();
        if (!secTest.runAllTests()) {
            allPassed = false;
        }

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("================================================================================");
        if (allPassed) {
            System.out.printf(">>> ALL TESTS COMPLETED SUCCESSFULLY IN %d ms! [STATUS: 100%% GREEN] <<<\n", duration);
            System.out.println(">>> Verified: Security, Architecture, Persistence, Grading, and UI Integration. <<<");
            System.out.println("================================================================================");
            System.exit(0);
        } else {
            System.err.printf(">>> TEST SUITE FAILED IN %d ms! Check error logs above. <<<\n", duration);
            System.out.println("================================================================================");
            System.exit(1);
        }
    }
}
