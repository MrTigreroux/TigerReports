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
		String name = toString();
		return name.charAt(0)+name.substring(1).toLowerCase();
	}

	public String getWord(String processor) {
		String w = word.get();
		return processor != null ? w.replace("_Name_", processor) : w;
	}

	public Material getMaterial() {
		return material;
	}

	public short getColor() {
		return color;
	}

	public static Status getFrom(String status) {
		try {
			return status == null	? Status.WAITING
									: status.startsWith(Status.DONE.getConfigWord())	? Status.DONE
																						: Status.valueOf(status.toUpperCase().replace(" ", "_"));
		} catch (Exception invalidStatus) {
			return Status.WAITING;
		}
	}

}
