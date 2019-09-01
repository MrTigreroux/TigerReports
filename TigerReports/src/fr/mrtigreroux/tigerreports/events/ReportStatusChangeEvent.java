package fr.mrtigreroux.tigerreports.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrtigreroux.tigerreports.objects.Report;

/**
 * @author MrTigreroux
 */

public class ReportStatusChangeEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private Report r;
	private String status;

	public ReportStatusChangeEvent(Report r, String status) {
		this.r = r;
		this.status = status;
	}

	public Report getReport() {
		return r;
	}

	public String getStatus() {
		return status;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
