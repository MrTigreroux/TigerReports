package fr.mrtigreroux.tigerreports.utils;

import static org.mockito.ArgumentMatchers.any;

import org.bukkit.Sound;
import org.mockito.MockedStatic;

import fr.mrtigreroux.tigerreports.data.Holder;

/**
 * @author MrTigreroux
 */
public class TestsMessageUtils {

	public static void mockSendStaffMessage(MockedStatic<MessageUtils> messageUtilsMock,
	        Holder<Object> sentStaffMessage) {
		messageUtilsMock.when(() -> MessageUtils.sendStaffMessage(any(Object.class), any(Sound.class)))
		        .then((invocation) -> {
			        Object msg = invocation.getArgument(0);
			        sentStaffMessage.set(msg);
			        return true;
		        });
	}

}
