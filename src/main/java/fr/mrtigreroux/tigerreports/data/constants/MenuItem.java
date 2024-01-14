package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.utils.VersionUtils;

/**
 * @author MrTigreroux
 */

public enum MenuItem {
    
    CLOSE(VersionUtils.isOldVersion() ? Material.REDSTONE_BLOCK : Material.BARRIER, Message.CLOSE),
    PAGE_SWITCH_PREVIOUS(Material.FEATHER, Message.PAGE_SWITCH_PREVIOUS),
    PAGE_SWITCH_NEXT(Material.FEATHER, Message.PAGE_SWITCH_NEXT),
    
    REASONS(Material.BOOK, Message.REASONS),
    REPORTS(Material.BOOKSHELF, Message.REPORTS),
    ARCHIVED_REPORTS(Material.BOOKSHELF, Message.ARCHIVED_REPORTS),
    DATA(Material.ENCHANTED_BOOK, Message.DATA),
    DELETE(Material.FLINT_AND_STEEL, Message.DELETE, Message.DELETE_DETAILS.get(), false),
    COMMENTS(Material.WRITTEN_BOOK, Message.COMMENTS, true),
    CANCEL_PROCESS(
            Material.FEATHER,
            Message.CANCEL_PROCESS,
            Message.CANCEL_PROCESS_DETAILS.get(),
            false
    );
    
    public static CustomItem ARCHIVE, PUNISHMENTS, PUNISH_ABUSE, WRITE_COMMENT;
    
    private final Material material;
    private final short durability;
    private final Message name;
    private final String details;
    private final boolean hideFlags;
    
    MenuItem(Material material, Message name) {
        this(material, name, null, false);
    }
    
    MenuItem(Material material, Message name, boolean hideFlags) {
        this(material, name, null, hideFlags);
    }
    
    MenuItem(Material material, Message name, String details, boolean hideFlags) {
        this(material, (short) 0, name, details, hideFlags);
    }
    
    MenuItem(Material material, short durability, Message name, String details, boolean hideFlags) {
        this.material = material;
        this.durability = durability;
        this.name = name;
        this.details = details;
        this.hideFlags = hideFlags;
    }
    
    public CustomItem getCustomItem() {
        return new CustomItem().name(name.get())
                .type(material)
                .damage(durability)
                .hideFlags(hideFlags);
    }
    
    public ItemStack getWithDetails(String details) {
        return getCustomItem().details(details).create();
    }
    
    public ItemStack get() {
        return getWithDetails(details);
    }
    
    public static void init() {
        ARCHIVE = getCustomItem(
                MenuRawItem.BLACK_CLAY.clone(),
                Message.ARCHIVE,
                Message.ARCHIVE_DETAILS
        );
        PUNISHMENTS = getCustomItem(MenuRawItem.GOLDEN_AXE.clone(), Message.PUNISHMENTS);
        PUNISH_ABUSE = getCustomItem(MenuRawItem.GOLDEN_AXE.clone(), Message.PUNISH_ABUSE);
        WRITE_COMMENT = getCustomItem(
                MenuRawItem.WRITABLE_BOOK.clone(),
                Message.WRITE_COMMENT,
                Message.WRITE_COMMENT_DETAILS
        );
    }
    
    private static CustomItem getCustomItem(CustomItem rawItem, Message name) {
        return rawItem.name(name.get()).hideFlags(true);
    }
    
    private static CustomItem getCustomItem(CustomItem rawItem, Message name, Message details) {
        return getCustomItem(rawItem, name).details(details.get());
    }
    
}
