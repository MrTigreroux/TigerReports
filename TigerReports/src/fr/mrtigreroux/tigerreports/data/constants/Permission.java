package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.command.CommandSender;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public enum Permission {

	REPORT, STAFF, TELEPORT, EXEMPT, ARCHIVE, REMOVE, ADVANCED, MANAGE;
	
	public String get() {
		return "tigerreports."+toString().toLowerCase();
	}
	
	public boolean isOwned(OnlineUser u) {
		return u.getPlayer().hasPermission(get());
	}
	
	public boolean check(CommandSender s) {
		if(!s.hasPermission(get())) {
			MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
			return false;
		}
		return true;
	}
	
}
