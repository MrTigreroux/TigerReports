package fr.mrtigreroux.tigerreports;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;

import fr.mrtigreroux.tigerreports.logs.Level;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.reports.TestsReport;
import fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler;
import fr.mrtigreroux.tigerreports.utils.TestsReportUtils;

/**
 * @author MrTigreroux
 */
@TestInstance(Lifecycle.PER_CLASS)
public abstract class TestClass {
    
    private static final byte MAX_PRINTED_PARENT_CLASSES = 4;
    
    protected final Logger LOGGER;
    
    public TestClass() {
        LOGGER = TigerReportsMock.getLoggerFromClass(getClass());
        LOGGER.setLevel(Level.INFO);
    }
    
    @BeforeEach
    void beforeTest(TestInfo testInfo) {
        LOGGER.info(() -> "Start of " + getTestName(testInfo));
    }
    
    @AfterEach
    void afterTest(TestInfo testInfo) {
        assertTrue(TestsTaskScheduler.cleanMainTaskSchedulerAfterUse());
        TestsReport.worlds.clear();
        TestsReportUtils.resetIndependentReportsManager();
        LOGGER.info(() -> "End of " + getTestName(testInfo));
    }
    
    String getTestName(TestInfo testInfo) {
        StringBuilder classNamePrefix = new StringBuilder();
        Class<?> testClass = testInfo.getTestClass().orElseThrow(IllegalStateException::new);
        byte i = 0;
        while (i < MAX_PRINTED_PARENT_CLASSES && testClass != null && testClass.isMemberClass()) {
            classNamePrefix.insert(0, testClass.getSimpleName() + ".");
            i++;
            testClass = testClass.getDeclaringClass();
        }
        String parametersInfo;
        if (
            testInfo.getTestMethod()
                    .orElseThrow(IllegalStateException::new)
                    .getAnnotation(ParameterizedTest.class) != null
        ) {
            parametersInfo = testInfo.getDisplayName();
        } else {
            parametersInfo = "";
        }
        return classNamePrefix + testInfo.getTestMethod()
                .orElseThrow(IllegalStateException::new)
                .getName() + "(" + parametersInfo + ")";
    }
    
}
