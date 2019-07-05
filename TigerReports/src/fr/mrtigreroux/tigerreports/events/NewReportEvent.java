package fr.mrtigreroux.tigerreports.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrtigreroux.tigerreports.objects.Report;

/**
 * @author MrTigreroux
 */

public class NewReportEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private String server;
	private Report r;

	public NewReportEvent(String server, Report r) {
		this.server = server;
		this.r = r;
	}

	public String getServer() {
		return server;
	}

	public Report getReport() {
		return r;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
