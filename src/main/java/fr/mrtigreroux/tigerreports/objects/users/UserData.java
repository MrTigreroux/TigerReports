package fr.mrtigreroux.tigerreports.objects.users;

import fr.mrtigreroux.tigerreports.managers.VaultManager;

/**
 * @author MrTigreroux
 */
public interface UserData {
    
    public String getName();
    
    public String getDisplayName(VaultManager vm);
    
}
