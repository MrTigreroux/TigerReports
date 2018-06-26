package fr.mrtigreroux.tigerreports.objects.users;

/**
 * @author MrTigreroux
 */

public class OfflineUser extends User {

	public OfflineUser(String uuid) {
		super(uuid);
	}

	@Override
	public void sendMessage(Object message) {}

}
