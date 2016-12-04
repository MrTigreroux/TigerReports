package fr.mrtigreroux.tigerreports.data;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.objects.CustomItem;

public enum Statistic {

	TRUE_APPRECIATION(20, "Appreciations.True", Message.APPRECIATION, Message.TRUE, new CustomItem().type(Material.STAINED_CLAY).damage((short) 5)),
	UNCERTAIN_APPRECIATION(22, "Appreciations.Uncertain", Message.APPRECIATION, Message.UNCERTAIN, new CustomItem().type(Material.STAINED_CLAY).damage((short) 4)),
	FALSE_APPRECIATION(24, "Appreciations.False", Message.APPRECIATION, Message.FALSE, new CustomItem().type(Material.STAINED_CLAY).damage((short) 14)),
	REPORTS(38, "Reports", Message.REPORTS_STATISTIC, new CustomItem().type(Material.PAPER)),
	REPORTED_TIME(40, "ReportedTime", Message.REPORTED_TIME_STATISTIC, new CustomItem().type(Material.BOW)),
	PROCESSED_REPORTS(42, "ProcessedReports", Message.PROCESSED_REPORTS_STATISTIC, new CustomItem().type(Material.BOOK));
	
	private int position;
	private String configName;
	private Message name;
	private Message appreciation;
	private CustomItem item;
	
	Statistic(int position, String configName, Message name, CustomItem item) {
		this(position, configName, name, null, item);
	}
	
	Statistic(int position, String configName, Message name, Message appreciation, CustomItem item) {
		this.position = position;
		this.configName = configName;
		this.name = name;
		this.appreciation = appreciation;
		this.item = item;
	}
	
	public int getPosition() {
		return position;
	}
	
	public String getConfigName() {
		return configName;
	}
	
	public String getName() {
		String name = this.name.get();
		if(appreciation != null) name = name.replace("_Appreciation_", appreciation.get());
		return name;
	}
	
	public CustomItem getItem() {
		return item;
	}
}
