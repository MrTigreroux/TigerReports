package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class ReasonMenu extends Menu {
	
	final User tu;
	
	public ReasonMenu(OnlineUser u, int page, User tu) {
		super(u, 54, page, null);
		this.tu = tu;
	}
	
	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.REASON_TITLE.get().replace("_Target_", tu.getName()), true);
		
		inv.setItem(4, MenuItem.REASONS.get());
		int firstReason = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstReason += (page-1)*27;
		}
		
		for(int reasonIndex = firstReason; reasonIndex <= firstReason+26; reasonIndex++) {
			String path = "Config.DefaultReasons.Reason"+reasonIndex;
			if(!ConfigUtils.exist(ConfigFile.CONFIG.get(), path))
				break;
			Material material = ConfigUtils.getMaterial(ConfigFile.CONFIG.get(), path+".Item");
			String reason = ConfigFile.CONFIG.get().getString(path+".Name");
			inv.setItem(reasonIndex-firstReason+18, new CustomItem()
					.type(material != null ? material : Material.PAPER)
					.damage(ConfigUtils.getDamage(ConfigFile.CONFIG.get(), path+".Item"))
					.skullOwner(ConfigUtils.getSkull(ConfigFile.CONFIG.get(), path+".Item"))
					.name(Message.REASON.get().replace("_Reason_", reason))
					.lore(Message.REASON_DETAILS.get()
							.replace("_Player_", tu.getName())
							.replace("_Reason_", reason)
							.split(ConfigUtils.getLineBreakSymbol()))
							.hideFlags(true).create());
		}
		
		if(ConfigUtils.exist(ConfigFile.CONFIG.get(), "Config.DefaultReasons.Reason"+(firstReason+27)))
			inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		
		return inv;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot >= 18 && slot <= size-9) {
			ConfigSound.MENU.play(p);
			p.chat("/tigerreports:report "+tu.getName()+" "+ConfigFile.CONFIG.get().getString("Config.DefaultReasons.Reason"+(getIndex(slot))+".Name"));
			p.closeInventory();
		}
	}
	
}
