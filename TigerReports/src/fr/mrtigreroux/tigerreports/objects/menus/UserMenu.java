package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class UserMenu extends Menu implements UpdatedMenu {

	private User tu;

	public UserMenu(OnlineUser u, User tu) {
		super(u, 54, 0, Permission.STAFF);
		this.tu = tu;
	}

	@Override
	public Inventory onOpen() {
		String name = tu.getName();
		Inventory inv = getInventory(Message.USER_TITLE.get().replace("_Target_", name), true);

		inv.setItem(4, new CustomItem().skullOwner(name).name(Message.USER.get().replace("_Target_", name)).create());

		setReportsItem(inv, "Menus.User-reports", 18, name);
		setReportsItem(inv, "Menus.User-archived-reports", 36, name);
		setReportsItem(inv, "Menus.User-against-reports", 26, name);
		setReportsItem(inv, "Menus.User-against-archived-reports", 44, name);

		return inv;
	}

	private void setReportsItem(Inventory inv, String tag, int slot, String name) {
		inv.setItem(slot,
				new CustomItem().type(Material.BOOKSHELF).name(Message.get(tag).replace("_Target_", name)).lore(
						Message.get(tag + "-details").replace("_Target_", name).split(ConfigUtils.getLineBreakSymbol()))
						.create());
	}

	@Override
	public void onUpdate(Inventory inv) {
		TigerReports tr = TigerReports.getInstance();
		Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

			@Override
			public void run() {
				String cooldown = tu.getCooldown();
				Map<String, Integer> statistics = tu.getStatistics();
				Bukkit.getScheduler().runTask(tr, new Runnable() {

					@Override
					public void run() {
						String lineBreak = ConfigUtils.getLineBreakSymbol();
						inv.setItem(8,
								MenuRawItem.GOLDEN_AXE
										.name(Message.COOLDOWN_STATUS.get().replace("_Time_",
												cooldown != null ? cooldown : Message.NONE_FEMALE.get()))
										.lore(cooldown != null
												? Message.COOLDOWN_STATUS_DETAILS.get()
														.replace("_Player_", tu.getName()).split(lineBreak)
												: null)
										.create());

						boolean hasManagePerm = Permission.MANAGE.isOwned(u);
						String userStatMessage = Message.USER_STATISTIC.get();
						String[] userStatDetails = Message.USER_STATISTIC_DETAILS.get().split(lineBreak);
						for (Statistic stat : Statistic.values()) {
							int value = statistics.get(stat.getConfigName());
							inv.setItem(stat.getPosition(),
									stat.getCustomItem().amount(value)
											.name(userStatMessage.replace("_Statistic_", stat.getName())
													.replace("_Amount_", Integer.toString(value)))
											.lore(hasManagePerm ? userStatDetails : null).create());
						}
					}

				});
			}
		});
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		switch (slot) {
		case 8:
			if (tu.getCooldown() != null) {
				tu.stopCooldown(p.getName(), false);
				update(false);
			}
			break;
		case 18:
			u.openUserReportsMenu(tu, 1);
			break;
		case 26:
			u.openUserAgainstReportsMenu(tu, 1);
			break;
		case 36:
			u.openUserArchivedReportsMenu(tu, 1);
			break;
		case 44:
			u.openUserAgainstArchivedReportsMenu(tu, 1);
			break;
		default:
			if (slot != 4 && Permission.MANAGE.isOwned(u)) {
				String stat = "";
				for (Statistic statistics : Statistic.values())
					if (statistics.getPosition() == slot)
						stat = statistics.getConfigName();
				tu.changeStatistic(stat, click.toString().contains("RIGHT") ? -1 : 1);
				update(true);
			}
			break;
		}
	}

}
