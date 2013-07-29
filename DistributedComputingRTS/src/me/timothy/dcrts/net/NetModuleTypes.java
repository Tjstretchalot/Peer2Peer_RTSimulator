package me.timothy.dcrts.net;

import java.util.HashMap;
import java.util.Map;


/**
 * Allows classes to get a list of available net modules at 
 * runtime. Primarily used in the {@link NetState#getNetTypeOf(me.timothy.dcrts.peer.Peer)}
 * 
 * @author Timothy
 */
public class NetModuleTypes {
	private static Map<String, NetModule> netModules;
	
	static {
		netModules = new HashMap<>();
	}
	
	/**
	 * Returns the net module registered with the specified
	 * string. Generally, the string is the same as the 
	 * canonical class name
	 * 
	 * @param str the string
	 * @return the net module registered with {@code str}
	 */
	public static NetModule getNetModule(String str) {
		return netModules.get(str);
	}
	
	/**
	 * Registers the specified net module using it's canonical class
	 * name
	 * @param netModule the net module
	 */
	public static void registerNetModule(NetModule netModule) {
		registerNetModule(netModule, netModule.getClass().getCanonicalName());
	}
	
	/**
	 * Registers the specified net module under a custom name. 
	 * 
	 * @param netModule The net module
	 * @param str the string
	 */
	public static void registerNetModule(NetModule netModule, String str) {
		netModules.put(str, netModule);
	}
}
