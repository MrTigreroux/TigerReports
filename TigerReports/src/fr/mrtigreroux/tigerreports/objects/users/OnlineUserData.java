package fr.mrtigreroux.tigerreports.objects.users;

import java.util.Objects;

import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.menus.Menu;
import fr.mrtigreroux.tigerreports.objects.reports.Report;

/**
 * @author MrTigreroux
 */
public class OnlineUserData implements UserData {

	final Player p;
	Menu openedMenu = null;
	Comment editingComment = null;
	PendingProcessPunishingData pendingProcessPunishingData = null;
	boolean notifications = true;

	public OnlineUserData(Player p) {
		this.p = Objects.requireNonNull(p);
	}

	@Override
	public String getName() {
		return p.getName();
	}

	@Override
	public String getDisplayName(VaultManager vm) {
		return vm.getOnlinePlayerDisplayName(p);
	}

	@Override
	public String getDisplayName(VaultManager vm, boolean staff) {
		return vm.getOnlinePlayerDisplayName(p, staff);
	}

	static class PendingProcessPunishingData {

		final Report r;
		final String punishmentConfigPath;

		public PendingProcessPunishingData(Report r, String punishmentConfigPath) {
			super();
			this.r = Objects.requireNonNull(r);
			this.punishmentConfigPath = Objects.requireNonNull(punishmentConfigPath);
		}

	}

}
