package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.CustomItem;

/**
 * @author MrTigreroux
 */

public enum Statistic {

	TRUE_APPRECIATIONS(20, Message.APPRECIATION, Message.TRUE, new CustomItem().type(Material.STAINED_CLAY).damage((short) 5)),
	UNCERTAIN_APPRECIATIONS(22, Message.APPRECIATION, Message.UNCERTAIN, new CustomItem().type(Material.STAINED_CLAY).damage((short) 4)),
	FALSE_APPRECIATIONS(24, Message.APPRECIATION, Message.FALSE, new CustomItem().type(Material.STAINED_CLAY).damage((short) 14)),
	REPORTS(38, Message.REPORTS_STATISTIC, new CustomItem().type(Material.PAPER)),
	REPORTED_TIMES(40, Message.REPORTED_TIMES_STATISTIC, new CustomItem().type(Material.BOW)),
	PROCESSED_REPORTS(42, Message.PROCESSED_REPORTS_STATISTIC, new CustomItem().type(Material.BOOK));
	
	private final int position;
	private final Message name;
	private final Message appreciation;
	private final CustomItem item;
	
	Statistic(int position, Message name, CustomItem item) {
		this(position, name, null, item);
	}
	
	Statistic(int position, Message name, Message appreciation, CustomItem item) {
		this.position = position;
		this.name = name;
		this.appreciation = appreciation;
		this.item = item;
	}
	
	public int getPosition() {
		return position;
	}
	
	public String getConfigName() {
		return toString().toLowerCase();
	}
	
	public String getName() {
		return appreciation != null ? name.get().replace("_Appreciation_", appreciation.get()) : name.get();
	}
	
	public CustomItem getItem() {
		return item;
	}
	
}
