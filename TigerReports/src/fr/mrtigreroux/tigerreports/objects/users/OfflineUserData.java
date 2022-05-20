package fr.mrtigreroux.tigerreports.objects.users;

import java.util.Objects;

import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;

/**
 * @author MrTigreroux
 */
public class OfflineUserData implements UserData {

	private String name;
	private String displayName;
	/**
	 * Last day this cached instance was used.
	 */
	protected byte lastDayUsed;

	public OfflineUserData(String name, String displayName) {
		this.name = Objects.requireNonNull(name);
		this.displayName = displayName != null && !displayName.isEmpty() ? displayName : name;
		lastDayUsed = DatetimeUtils.getCurrentDayOfMonth();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName(VaultManager vm) {
		return displayName;
	}

	@Override
	public String getDisplayName(VaultManager vm, boolean staff) {
		return vm.useVaultDisplayName(staff) ? displayName : name;
	}

}
