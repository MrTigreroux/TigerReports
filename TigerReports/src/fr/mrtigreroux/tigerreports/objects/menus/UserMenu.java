package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class UserMenu extends Menu {
	
	public UserMenu(User u, String target) {
		super(u, 54, 0, 0, null, target);
	}
	
	@Override
	public void open(boolean sound) {
		String name = UserUtils.getName(target);
		Inventory inv = getInventory(Message.USER_TITLE.get().replaceAll("_Target_", name), true);
		
		inv.setItem(4, new CustomItem().skullOwner(UserUtils.getName(target)).name(Message.USER.get().replaceAll("_Target_", name)).create());
		try {
			String cooldown = new User(UserUtils.getPlayer(name)).getCooldown();
			inv.setItem(8, new CustomItem().type(Material.GOLD_AXE).hideFlags(true).name(Message.COOLDOWN_STATUS.get().replaceAll("_Time_", cooldown != null ? cooldown : Message.NONE_FEMALE.get()))
					.lore(cooldown != null ? Message.COOLDOWN_STATUS_DETAILS.get().replaceAll("_Player_", name).split(ConfigUtils.getLineBreakSymbol()) : null).create());
		} catch(Exception playerOffline) {
			;
		}
		
		for(String appreciation : Arrays.asList("True", "Uncertain", "False")) {
			String appreciationWord = Message.valueOf(appreciation.toUpperCase()).get();
			inv.setItem(appreciation.equals("True") ? 29 : appreciation.equals("Uncertain") ? 31 : 33, new CustomItem().type(Material.STAINED_CLAY)
					.amount(UserUtils.getAppreciation(target, appreciation)).damage((byte) (appreciation.equals("True") ? 5 : appreciation.equals("Uncertain") ? 4 : 14))
					.name(Message.USER_APPRECIATION.get().replaceAll("_Appreciation_", appreciationWord).replaceAll("_Number_", ""+UserUtils.getAppreciation(target, appreciation))).lore(u.hasPermission(Permission.ADVANCED) ? Message.USER_APPRECIATION_DETAILS.get().split(ConfigUtils.getLineBreakSymbol()) : null).create());
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
				User tu = new User(t);
				if(tu.getCooldown() != null) {
					tu.stopCooldown(p.getName());
					open(false);
				}
			} catch(Exception playerOffline) {
				;
			}
		} else if((slot == 29 || slot == 31 || slot == 33) && u.hasPermission(Permission.ADVANCED)) {
			UserUtils.addAppreciation(target, slot == 29 ? "True" : slot == 31 ? "Uncertain" : "False", click.toString().contains("RIGHT") ? -1 : 1);
			open(true);
		}
	}
	
}
