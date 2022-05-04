package fr.mrtigreroux.tigerreports.objects.users;

import fr.mrtigreroux.tigerreports.managers.VaultManager;

public interface UserData {

	public String getName();

	public String getDisplayName(VaultManager vm);

	public String getDisplayName(VaultManager vm, boolean staff);

}
