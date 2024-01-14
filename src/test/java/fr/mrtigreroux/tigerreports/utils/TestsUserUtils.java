package fr.mrtigreroux.tigerreports.utils;

import static org.mockito.ArgumentMatchers.anyBoolean;

import org.mockito.MockedStatic;

/**
 * @author MrTigreroux
 */
public class TestsUserUtils {
    
    public static void mockUseDisplayName(MockedStatic<UserUtils> userUtilsMock, boolean value) {
        userUtilsMock.when(() -> UserUtils.useDisplayName(anyBoolean())).then((invocation) -> {
            return value;
        });
    }
    
}
