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
	private final boolean bungee;

	public ProcessReportEvent(Report r, String staff, boolean bungee) {
		this.r = Objects.requireNonNull(r);
		this.staff = staff;
		this.bungee = bungee;
	}

	public Report getReport() {
		return r;
	}

	public String getStaff() {
		return staff;
	}

	public boolean isFromBungeeCord() {
		return bungee;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
