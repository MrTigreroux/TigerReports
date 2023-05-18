package fr.mrtigreroux.tigerreports.objects.users;

import java.util.Objects;

import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;

/**
 * @author MrTigreroux
 */
public class OfflineUserData implements UserData {

	private String name;
	/**
	 * Last day this cached instance was used.
	 */
	protected byte lastDayUsed;

	public OfflineUserData(String name) {
		this.name = Objects.requireNonNull(name);
		lastDayUsed = DatetimeUtils.getCurrentDayOfMonth();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName(VaultManager vm) {
		return null;
	}

}
