package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.CustomItem;

/**
 * @author MrTigreroux
 */

public enum Status {

	WAITING("Waiting", Message.WAITING, Material.PAPER, MenuRawItem.GREEN_CLAY.clone()),
	IN_PROGRESS("In_progress", Message.IN_PROGRESS, Material.PAPER, MenuRawItem.YELLOW_CLAY.clone()),
	IMPORTANT("Important", Message.IMPORTANT, MenuRawItem.EMPTY_MAP.clone(), MenuRawItem.RED_CLAY.clone()),
	DONE("Done", Message.DONE, Material.BOOK, MenuRawItem.BLUE_CLAY);

	private final String configName;
	private final Message displayName;
	private final Material iconMat;
	private CustomItem icon;
	private final CustomItem buttonItem;

	public static Status from(String statusDetails) {
		try {
			if (statusDetails == null) {
				return Status.WAITING;
			} else if (statusDetails.startsWith(Status.DONE.getConfigName())) {
				return Status.DONE;
			} else if (statusDetails.startsWith(Status.IN_PROGRESS.getConfigName())) {
				return Status.IN_PROGRESS;
			} else {
				return fromConfigName(statusDetails.split(" ")[0]);
			}
		} catch (Exception invalidStatus) {
			return Status.WAITING;
		}
	}

	public static Status fromConfigName(String configName) {
		return Status.valueOf(configName.toUpperCase());
	}

	Status(String configName, Message word, CustomItem icon, CustomItem buttonItem) {
		this(configName, word, (Material) null, buttonItem);
		this.icon = icon;
	}

	Status(String configName, Message word, Material iconMat, CustomItem buttonItem) {
		this.configName = configName;
		displayName = word;
		this.iconMat = iconMat;
		this.buttonItem = buttonItem;
	}

	public String getConfigName() {
		return configName;
	}

	public String getDisplayName(String staffName) {
		return getDisplayName(staffName, false);
	}

	public String getDisplayName(String staffName, boolean detailed) {
		String w = detailed && this == Status.IN_PROGRESS ? Message.get("Words.In-progress-detailed")
		        : displayName.get();
		return staffName != null ? w.replace("_Name_", staffName) : w;
	}

	public CustomItem getIcon() {
		return iconMat != null ? new CustomItem().type(iconMat) : icon;
	}

	public CustomItem getButtonItem() {
		return buttonItem.clone();
	}

	@Override
	public String toString() {
		return configName;
	}

}
