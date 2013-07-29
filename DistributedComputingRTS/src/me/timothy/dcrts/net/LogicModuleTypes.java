package me.timothy.dcrts.net;

import java.util.HashMap;
import java.util.Map;


/**
 * Allows classes to get a list of available logic modules at 
 * runtime. Primarily used in the {@link NetState#getLogicTypeOf(me.timothy.dcrts.peer.Peer)}
 * 
 * @author Timothy
 */
public class LogicModuleTypes {
	private static Map<String, LogicModule> logicModules;
	
	static {
		logicModules = new HashMap<>();
	}
	
	/**
	 * Returns the logic module registered with the specified
	 * string. Generally, the string is the same as the 
	 * canonical class name
	 * 
	 * @param str the string
	 * @return the logic module registered with {@code str}
	 */
	public static LogicModule getLogicModule(String str) {
		return logicModules.get(str);
	}
	
	/**
	 * Registers the specified logic module using it's canonical class
	 * name
	 * @param logicModule the logic module
	 */
	public static void registerLogicModule(LogicModule logicModule) {
		registerLogicModule(logicModule, logicModule.getClass().getCanonicalName());
	}
	
	/**
	 * Registers the specified logic module under a custom name. 
	 * 
	 * @param logicModule The logic module
	 * @param str the string
	 */
	public static void registerLogicModule(LogicModule logicModule, String str) {
		logicModules.put(str, logicModule);
	}
}
