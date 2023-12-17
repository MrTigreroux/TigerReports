package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.Arrays;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public class TestsBungeeNotification extends BungeeNotification {

	public final boolean bool;
	public final byte b;
	public final short s;
	public final int i;
	public final float f;
	public final double d;
	public final long l;
	public final String str;
	public final String[] strArray;

	public TestsBungeeNotification(long creationTime, boolean bool, byte b, short s, int i, float f, double d, long l,
	        String str, String[] strArray) {
		super(creationTime);
		this.bool = bool;
		this.b = b;
		this.s = s;
		this.i = i;
		this.f = f;
		this.d = d;
		this.l = l;
		this.str = str;
		this.strArray = strArray;
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}

	@Override
	public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm, BungeeManager bm) {}

	@Override
	public String toString() {
		return "TestsBungeeNotification [bool=" + bool + ", b=" + b + ", s=" + s + ", i=" + i + ", f=" + f + ", d=" + d
		        + ", l=" + l + ", str=" + str + ", strArray=" + Arrays.toString(strArray) + "]";
	}

}
