package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.data.config.Message;

/**
 * @author MrTigreroux
 */

public enum Status {

	WAITING(Message.WAITING, Material.PAPER, (short) 5),
	IN_PROGRESS(Message.IN_PROGRESS, Material.PAPER, (short) 4),
	IMPORTANT(Message.IMPORTANT, Material.EMPTY_MAP, (short) 14),
	DONE(Message.DONE, Material.BOOK, (short) 3);
	
	private final Message word;
	private final Material material;
	private final short color;
	
	Status(Message word, Material material, short color) {
		this.word = word;
		this.material = material;
		this.color = color;
	}

	public String getConfigWord() {
		return toString().substring(0, 1).toUpperCase()+toString().substring(1).toLowerCase().replace("_", " ");
	}
	
	public String getWord(String processor) {
		return processor != null ? word.get().replace("_Name_", processor) : word.get();
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public short getColor() {
		return color;
	}
	
	public static Status getFrom(String status) {
		try {
			return status == null ? Status.WAITING : status.startsWith(Status.DONE.getConfigWord()) ? Status.DONE : Status.valueOf(status.toUpperCase().replace(" ", "_"));
		} catch (Exception invalidStatus) {
			return Status.WAITING;
		}
	}
	
}
