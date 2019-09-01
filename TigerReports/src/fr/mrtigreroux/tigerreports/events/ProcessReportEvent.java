package fr.mrtigreroux.tigerreports.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrtigreroux.tigerreports.objects.Report;

/**
 * @author MrTigreroux
 */

public class ProcessReportEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private Report r;
	private String staff;

	public ProcessReportEvent(Report r, String staff) {
		this.r = r;
		this.staff = staff;
	}

	public Report getReport() {
		return r;
	}

	public String getStaff() {
		return staff;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
