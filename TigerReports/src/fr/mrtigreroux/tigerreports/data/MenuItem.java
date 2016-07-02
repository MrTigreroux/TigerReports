package fr.mrtigreroux.tigerreports.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public enum MenuItem {

	CLOSE(0, Material.BARRIER, Message.CLOSE.get()),
	PAGE_SWITCH_PREVIOUS(0, Material.FEATHER, Message.PAGE_SWITCH_PREVIOUS.get()),
	PAGE_SWITCH_NEXT(0, Material.FEATHER, Message.PAGE_SWITCH_NEXT.get()),

	REPORTS_ICON(0, Material.BOOKSHELF, Message.REPORTS.get()),
	PUNISH_ABUSE(22, Material.GOLD_AXE, Message.PUNISH_ABUSE.get(), true),
	DATA(26, Material.ENCHANTED_BOOK, Message.DATA.get()),
	ARCHIVE(33, Material.STAINED_CLAY, (short) 9, Message.ARCHIVE.get(), Message.ARCHIVE_DETAILS.get(), false),
	REMOVE(36, Material.FLINT_AND_STEEL, Message.REMOVE.get(), Message.REMOVE_DETAILS.get(), false),
	COMMENTS(44, Material.WRITTEN_BOOK, Message.COMMENTS.get(), true),
	CANCEL_APPRECIATION(18, Material.FEATHER, Message.CANCEL_PROCESS.get(), Message.CANCEL_PROCESS_DETAILS.get(), false),
	WRITE_COMMENT(44, Material.BOOK_AND_QUILL, Message.WRITE_COMMENT.get(), Message.WRITE_COMMENT_DETAILS.get(), false),
	;
	
	private int position = 0;
	private Material material;
	private Short durability = 0;
	private String name = "";
	private String details = null;
	private boolean hideFlags = false;
	
	MenuItem(int position, Material material, String name) {
		this(position, material, name, null, false);
	}

	MenuItem(int position, Material material, String name, boolean hideFlags) {
		this(position, material, name, null, hideFlags);
	}
	
	MenuItem(int position, Material material, String name, String details, boolean hideFlags) {
		this(position, material, (short) 0, name, details, hideFlags);
	}

	MenuItem(int position, Material material, Short durability, String name, String details, boolean hideFlags) {
		this.position = position;
		this.material = material;
		this.durability = durability;
		this.name = name;
		this.details = details;
		this.hideFlags = hideFlags;
	}

	private CustomItem getCustomItem() {
		return new CustomItem().type(material).damage(durability).name(name).lore(details != null ? details.split(ConfigUtils.getLineBreakSymbol()) : null).hideFlags(hideFlags);
	}

	public ItemStack getWithDetails(String details) {
		this.details = details;
		ItemStack item = getCustomItem().create();
		this.details = null;
		return item;
	}
	
	public ItemStack get() {
		return getCustomItem().create();
	}
	
	public int getPosition() {
		return position;
	}
	
}
