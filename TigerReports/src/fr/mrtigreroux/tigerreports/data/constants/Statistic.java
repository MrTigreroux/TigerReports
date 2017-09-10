package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.CustomItem;

/**
 * @author MrTigreroux
 */

public enum Statistic {

	TRUE_APPRECIATIONS(20, Message.APPRECIATION, Message.TRUE, Material.STAINED_CLAY, (short)5),
	UNCERTAIN_APPRECIATIONS(22, Message.APPRECIATION, Message.UNCERTAIN, Material.STAINED_CLAY, (short)4),
	FALSE_APPRECIATIONS(24, Message.APPRECIATION, Message.FALSE, Material.STAINED_CLAY, (short)14),
	REPORTS(38, Message.REPORTS_STATISTIC, Material.PAPER),
	REPORTED_TIMES(40, Message.REPORTED_TIMES_STATISTIC, Material.BOW),
	PROCESSED_REPORTS(42, Message.PROCESSED_REPORTS_STATISTIC, Material.BOOK);
	
	private final int position;
	private final Message name, appreciation;
	private final Material material;
	private final short durability;
	
	Statistic(int position, Message name, Material material) {
		this(position, name, null, material, (short)0);
	}
	
	Statistic(int position, Message name, Message appreciation, Material material, short durability) {
		this.position = position;
		this.name = name;
		this.appreciation = appreciation;
		this.material = material;
		this.durability = durability;
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
	
	public CustomItem getCustomItem() {
		return new CustomItem().type(material).damage(durability);
	}
	
}
