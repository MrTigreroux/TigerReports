package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.Statistic;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class UserMenu extends Menu {
	
	public UserMenu(User u, String target) {
		super(u, 54, 0, null, null, target);
	}
	
	@Override
	public void open(boolean sound) {
		String name = UserUtils.getName(target);
		Inventory inv = getInventory(Message.USER_TITLE.get().replace("_Target_", name), true);
		
		inv.setItem(4, new CustomItem().skullOwner(UserUtils.getName(target)).name(Message.USER.get().replace("_Target_", name)).create());
		try {
			String cooldown = new User(UserUtils.getPlayer(name)).getCooldown();
			inv.setItem(8, new CustomItem().type(Material.GOLD_AXE).hideFlags(true).name(Message.COOLDOWN_STATUS.get().replace("_Time_", cooldown != null ? cooldown : Message.NONE_FEMALE.get()))
					.lore(cooldown != null ? Message.COOLDOWN_STATUS_DETAILS.get().replace("_Player_", name).split(ConfigUtils.getLineBreakSymbol()) : null).create());
		} catch(Exception playerOffline) {
			;
		}
		
		for(Statistic stat : Statistic.values()) {
			String configName = stat.getConfigName();
			inv.setItem(stat.getPosition(), stat.getItem()
					.amount(UserUtils.getStat(target, configName))
					.name(Message.USER_STATISTIC.get().replace("_Statistic_", stat.getName()).replace("_Number_", ""+UserUtils.getStat(target, configName)))
					.lore(u.hasPermission(Permission.ADVANCED) ? Message.USER_STATISTIC_DETAILS.get().split(ConfigUtils.getLineBreakSymbol()) : null).create());
		}
		
		p.openInventory(inv);
		if(sound) p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 8) {
			try {
				Player t = Bukkit.getPlayer(UUID.fromString(target));
				User tu = UserUtils.getUser(t);
				if(tu.getCooldown() != null) {
					tu.stopCooldown(p.getName());
					open(false);
				}
			} catch(Exception playerOffline) {
				;
			}
		} else if(u.hasPermission(Permission.ADVANCED)) {
			String stat = "";
			for(Statistic stats : Statistic.values()) if(stats.getPosition() == slot) stat = stats.getConfigName();
			UserUtils.changeStat(target, stat, click.toString().contains("RIGHT") ? -1 : 1);
			open(true);
		}
	}
	
}
