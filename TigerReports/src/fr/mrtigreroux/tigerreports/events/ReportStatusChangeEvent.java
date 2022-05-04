package fr.mrtigreroux.tigerreports.events;

import java.util.Objects;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrtigreroux.tigerreports.objects.reports.Report;

/**
 * @author MrTigreroux
 */

public class ReportStatusChangeEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Report r;

	public ReportStatusChangeEvent(Report r) {
		this.r = Objects.requireNonNull(r);
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
