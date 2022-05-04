package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.CustomItem;

/**
 * @author MrTigreroux
 */

public enum Status {

	WAITING(Message.WAITING, Material.PAPER, MenuRawItem.GREEN_CLAY.clone()),
	IN_PROGRESS(Message.IN_PROGRESS, Material.PAPER, MenuRawItem.YELLOW_CLAY.clone()),
	IMPORTANT(Message.IMPORTANT, MenuRawItem.EMPTY_MAP.clone(), MenuRawItem.RED_CLAY.clone()),
	DONE(Message.DONE, Material.BOOK, MenuRawItem.BLUE_CLAY);

	private final Message word;
	private final Material iconMat;
	private CustomItem icon;
	private final CustomItem buttonItem;

	public static Status from(String status) {
		try {
			if (status == null) {
				return Status.WAITING;
			} else if (status.startsWith(Status.DONE.getRawName())) {
				return Status.DONE;
			} else if (status.startsWith(Status.IN_PROGRESS.getRawName())) {
				return Status.IN_PROGRESS;
			} else {
				return fromRawName(status.split(" ")[0]);
			}
		} catch (Exception invalidStatus) {
			return Status.WAITING;
		}
	}

	public static Status fromRawName(String rawName) {
		return Status.valueOf(rawName.toUpperCase());
	}

	Status(Message word, CustomItem icon, CustomItem buttonItem) {
		this(word, (Material) null, buttonItem);
		this.icon = icon;
	}

	Status(Message word, Material iconMat, CustomItem buttonItem) {
		this.word = word;
		this.iconMat = iconMat;
		this.buttonItem = buttonItem;
	}

	public String getRawName() {
		String name = name();
		return name.charAt(0) + name.substring(1).toLowerCase();
	}

	public String getWord(String staffName) {
		return getWord(staffName, false);
	}

	public String getWord(String staffName, boolean detailed) {
		String w = detailed && this == Status.IN_PROGRESS ? Message.get("Words.In-progress-detailed") : word.get();
		return staffName != null ? w.replace("_Name_", staffName) : w;
	}

	public CustomItem getIcon() {
		return iconMat != null ? new CustomItem().type(iconMat) : icon;
	}

	public CustomItem getButtonItem() {
		return buttonItem.clone();
	}

}
