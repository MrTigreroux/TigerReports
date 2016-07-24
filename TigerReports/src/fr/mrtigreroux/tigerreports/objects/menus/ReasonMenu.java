package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.MenuItem;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class ReasonMenu extends Menu {
	
	public ReasonMenu(User u, int page, String target) {
		super(u, 54, page, 0, null, target);
	}
	
	@Override
	public void open(boolean sound) {
		Inventory inv = getInventory(Message.REASON_TITLE.get().replaceAll("_Target_", ""+target), true);
		
		inv.setItem(4, MenuItem.REASONS_ICON.get());
		int firstReason = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstReason = ((page-1)*27)+1;
		}
		for(int reasonNumber = firstReason; reasonNumber <= firstReason+26; reasonNumber++) {
			String path = "Config.DefaultReasons.Reason"+reasonNumber;
			if(!ConfigUtils.exist(FilesManager.getConfig, path)) break;
			Material material = ConfigUtils.getMaterial(FilesManager.getConfig, path+".Item");
			String reason = FilesManager.getConfig.getString(path+".Name");
			inv.setItem(reasonNumber-firstReason+18, new CustomItem().type(material != null ? material : Material.PAPER).damage(ConfigUtils.getDamage(FilesManager.getConfig, path+".Item")).skullOwner(ConfigUtils.getSkull(FilesManager.getConfig, path+".Item")).name(Message.REASON.get().replaceAll("_Reason_", reason)).lore(Message.REASON_DETAILS.get().replaceAll("_Player_", target).replaceAll("_Reason_", reason).split(ConfigUtils.getLineBreakSymbol())).hideFlags(true).create());
		}
		
		if(ConfigUtils.exist(FilesManager.getConfig, "Config.DefaultReasons.Reason"+(firstReason+27))) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		p.openInventory(inv);
		if(sound) p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot >= 18 && slot <= size-9) {
			p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
			p.chat("/report "+target+" "+FilesManager.getConfig.getString("Config.DefaultReasons.Reason"+(slot-18+((page-1)*27)+1)+".Name"));
			p.closeInventory();
		}
	}
	
}
