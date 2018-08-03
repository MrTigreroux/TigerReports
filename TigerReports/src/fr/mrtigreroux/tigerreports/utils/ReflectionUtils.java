package fr.mrtigreroux.tigerreports.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ReflectionUtils {

	private static double version = 0;
	
	public static String cbVer() {
		return "org.bukkit.craftbukkit."+ver()+".";
	}
	
	public static String nmsVer() {
		return "net.minecraft.server."+ver()+".";
	}
	
	public static String ver() {
		String pkg = Bukkit.getServer().getClass().getPackage().getName();
		return pkg.substring(pkg.lastIndexOf(".")+1);
    }
	
	private static double getVersion() {
		if(version == 0) {
			String ver = Bukkit.getVersion();
			ver = ver.substring(ver.indexOf('(')+5, ver.length()-1).replaceFirst("\\.", "");
			try {
				version = Double.parseDouble(ver+(StringUtils.countMatches(ver, ".") == 0 ? ".0" : ""));
			} catch (Exception ignored) {}
		}
		return version;
	}
	
	public static boolean isOldVersion() {
		return getVersion() < 18;
	}
	
	public static boolean isRecentVersion() {
		return getVersion() > 112.1;
	}
	
	public static Class<?> wrapperToPrimitive(Class<?> clazz) {
		if(clazz == Boolean.class) return boolean.class;
		if(clazz == Integer.class) return int.class;
		if(clazz == Double.class) return double.class;
		if(clazz == Float.class) return float.class;
		if(clazz == Long.class) return long.class;
		if(clazz == Short.class) return short.class;
		if(clazz == Byte.class) return byte.class;
		if(clazz == Void.class) return void.class;
		if(clazz == Character.class) return char.class;
		return clazz;
	}
	
	public static Class<?>[] toParamTypes(Object... params) {
		Class<?>[] classes = new Class<?>[params.length];
		for(int i = 0; i < params.length; i++) classes[i] = wrapperToPrimitive(params[i].getClass());
		return classes;
	}
	
	public static Object getHandle(Entity e) {
		return callMethod(e, "getHandle");
	}
	
	public static Object playerConnection(Player p) {
		return playerConnection(getHandle(p));
	}
	
	public static Object playerConnection(Object handle) {
		return getDeclaredField(handle, "playerConnection");
	}
	
	public static void sendPacket(Player p, Object packet) {
		Object pc = playerConnection(p);
		try {
		    pc.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(pc, packet);
		} catch(Exception e) {
		    e.printStackTrace();
		}
	}
	
	public static Object getPacket(String name, Object... params) {
		return callDeclaredConstructor(getNMSClass(name), params);
	}
	
	public static Class<?> getClass(String name) {
		try {
			return Class.forName(name);
		} catch (Exception ex) {
			return null;
		}
	}
	
	public static Class<?> getNMSClass(String name) {
		return getClass(nmsVer()+name);
	}
	
	public static Object callMethod(Object object, String method) {
		try {
			Method m = object.getClass().getMethod(method);
			m.setAccessible(true);
			return m.invoke(object);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static Object callSuperMethod(Object object, String method) {
		try {
			Method m = object.getClass().getSuperclass().getDeclaredMethod(method);
			m.setAccessible(true);
			return m.invoke(object);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static Object callDeclaredConstructor(Class<?> clazz, Object... params) {
		try {Constructor<?> con = clazz.getDeclaredConstructor(toParamTypes(params));
			con.setAccessible(true);
			return con.newInstance(params);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static Object getDeclaredField(Object object, String field) {
		try {
			Field f = object.getClass().getDeclaredField(field);
			f.setAccessible(true);
			return f.get(object);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static void setDeclaredField(Object object, String field, Object value) {
		try {
			Field f = object.getClass().getDeclaredField(field);
			f.setAccessible(true);
			f.set(object, value);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}