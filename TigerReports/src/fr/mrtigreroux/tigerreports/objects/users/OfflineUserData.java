package fr.mrtigreroux.tigerreports.objects.users;

import java.util.Objects;

import fr.mrtigreroux.tigerreports.managers.VaultManager;

public class OfflineUserData implements UserData {

	private String name;
	private String displayName;

	public OfflineUserData(String name, String displayName) {
		this.name = Objects.requireNonNull(name);
		this.displayName = displayName != null && !displayName.isEmpty() ? displayName : name;
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
