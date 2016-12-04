package fr.mrtigreroux.tigerreports.data;

import org.bukkit.Material;

/**
 * @author MrTigreroux
 */

public enum Status {

	WAITING(Message.WAITING, Material.PAPER, 5),
	IN_PROGRESS(Message.IN_PROGRESS, Material.PAPER, 4),
	IMPORTANT(Message.IMPORTANT, Material.EMPTY_MAP, 14),
	DONE(Message.DONE, Material.BOOK, 3);
	
	
	private final Message word;
	private final Material material;
	private final int color;
	
	Status(Message word, Material material, int color) {
		this.word = word;
		this.material = material;
		this.color = color;
	}

	public String getConfigWord() {
		return toString().substring(0, 1).toUpperCase()+toString().substring(1, toString().length()).toLowerCase().replace("_", " ");
	}
	
	public String getWord(String processor) {
		return processor != null ? word.get().replace("_Name_", processor) : word.get();
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public short getColor() {
		return (short) color;
	}
	
}
