package fr.mrtigreroux.tigerreports.data;

import org.bukkit.Material;

/**
 * @author MrTigreroux
 */

public enum Status {

	WAITING(Message.WAITING.get(), Material.PAPER, 5),
	IN_PROGRESS(Message.IN_PROGRESS.get(), Material.PAPER, 4),
	IMPORTANT(Message.IMPORTANT.get(), Material.EMPTY_MAP, 14),
	DONE(Message.DONE.get(), Material.BOOK, 3);
	
	
	private final String word;
	private final Material material;
	private final int color;
	
	Status(String word, Material material, int color) {
		this.word = word;
		this.material = material;
		this.color = color;
	}

	public String getConfigWord() {
		return toString().substring(0, 1).toUpperCase()+toString().substring(1, toString().length()).toLowerCase().replaceAll("_", " ");
	}
	
	public String getWord(String processor) {
		return processor != null ? word.replaceAll("_Name_", processor) : word;
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public short getColor() {
		return (short) color;
	}
	
}
