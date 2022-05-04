package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class UserMenu extends Menu implements UpdatedMenu, User.UserListener {

	private User tu;
	private final ReportsManager rm;
	private final Database db;
	private final TaskScheduler taskScheduler;
	private final VaultManager vm;
	private final UsersManager um;

	public UserMenu(User u, User tu, ReportsManager rm, Database db, TaskScheduler taskScheduler, VaultManager vm,
	        UsersManager um) {
		super(u, 54, 0, Permission.STAFF);
		this.tu = tu;
		this.rm = rm;
		this.db = db;
		this.taskScheduler = taskScheduler;
		this.vm = vm;
		this.um = um;
	}

	@Override
	public Inventory onOpen() {
		tu.addListener(this, db, taskScheduler, um);
		String name = tu.getName();
		String displayName = tu.getDisplayName(vm, false);
		Inventory inv = getInventory(Message.USER_TITLE.get().replace("_Target_", name), true);

		inv.setItem(4,
		        new CustomItem().skullOwner(name).name(Message.USER.get().replace("_Target_", displayName)).create());

		setReportsCategoryItem(inv, "Menus.User-reports", 18, displayName);
		setReportsCategoryItem(inv, "Menus.User-archived-reports", 36, displayName);
		setReportsCategoryItem(inv, "Menus.User-against-reports", 26, displayName);
		setReportsCategoryItem(inv, "Menus.User-against-archived-reports", 44, displayName);

		return inv;
	}

	private void setReportsCategoryItem(Inventory inv, String tag, int slot, String displayName) {
		inv.setItem(slot,
		        new CustomItem().type(Material.BOOKSHELF)
		                .name(Message.get(tag).replace("_Target_", displayName))
		                .lore(Message.get(tag + "-details")
		                        .replace("_Target_", displayName)
		                        .split(ConfigUtils.getLineBreakSymbol()))
		                .create());
	}

	@Override
	public void onUpdate(Inventory inv) {
		updateCooldownDisplay(inv);
		updateStatisticsDisplay(inv);
	}

	private void updateCooldownDisplay(Inventory inv) {
		String cooldown = tu.getCooldown();

		inv.setItem(8,
		        MenuRawItem.GOLDEN_AXE.clone()
		                .name(Message.COOLDOWN_STATUS.get()
		                        .replace("_Time_", cooldown != null ? cooldown : Message.NONE_FEMALE.get()))
		                .lore(cooldown != null
		                        ? Message.COOLDOWN_STATUS_DETAILS.get()
		                                .replace("_Player_", tu.getDisplayName(vm, false))
		                                .split(ConfigUtils.getLineBreakSymbol())
		                        : null)
		                .create());
	}

	private void updateStatisticsDisplay(Inventory inv) {
		Map<String, Integer> statistics = tu.getStatistics();

		boolean hasManagePerm = u.hasPermission(Permission.MANAGE);
		String userStatMessage = Message.USER_STATISTIC.get();
		String[] userStatDetails = Message.USER_STATISTIC_DETAILS.get().split(ConfigUtils.getLineBreakSymbol());
		boolean areStats = statistics != null;
		for (Statistic stat : Statistic.values()) {
			Integer value = null;
			if (areStats) {
				value = statistics.get(stat.getConfigName());
			}
			if (value == null) {
				value = 0;
			}
			inv.setItem(stat.getPosition(),
			        stat.getCustomItem()
			                .amount(value)
			                .name(userStatMessage.replace("_Statistic_", stat.getName())
			                        .replace("_Amount_", Integer.toString(value)))
			                .lore(hasManagePerm ? userStatDetails : null)
			                .create());
		}
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		TigerReports tr = TigerReports.getInstance();
		BungeeManager bm = tr.getBungeeManager();
		switch (slot) {
		case 8:
			tu.getCooldownAsynchronously(db, taskScheduler, new ResultCallback<String>() {

				@Override
				public void onResultReceived(String cooldown) {
					if (cooldown != null) {
						tu.stopCooldown(u, false, db, bm);
					}
				}

			});
			break;
		case 18:
			u.openUserReportsMenu(tu, 1, rm, db, taskScheduler, vm, bm, um);
			break;
		case 26:
			u.openUserAgainstReportsMenu(tu, 1, rm, db, taskScheduler, vm, bm, um);
			break;
		case 36:
			u.openUserArchivedReportsMenu(tu, 1, rm, db, taskScheduler, vm, bm, um);
			break;
		case 44:
			u.openUserAgainstArchivedReportsMenu(tu, 1, rm, db, taskScheduler, vm, bm, um);
			break;
		default:
			if (slot != 4 && u.hasPermission(Permission.MANAGE)) {
				Statistic stat = Statistic.getStatisticAtPosition(slot);
				if (stat != null) {
					tu.changeStatistic(stat, click.isRightClick() ? -1 : 1, db, bm);
				}
				ConfigSound.MENU.play(p);
			}
			break;
		}
	}

	@Override
	public void onCooldownChange(User tu) {
		update(false);
	}

	@Override
	public void onStatisticsChange(User tu) {
		update(false);
	}

	@Override
	public void onClose() {
		tu.removeListener(this);
		super.onClose();
	}

}
