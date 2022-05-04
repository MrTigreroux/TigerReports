package fr.mrtigreroux.tigerreports.events;

import java.util.Objects;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrtigreroux.tigerreports.objects.reports.Report;

/**
 * @author MrTigreroux
 */

public class ProcessReportEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Report r;
	private final String staff;

	public ProcessReportEvent(Report r, String staff) {
		this.r = Objects.requireNonNull(r);
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
