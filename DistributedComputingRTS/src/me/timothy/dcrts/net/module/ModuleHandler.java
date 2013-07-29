package me.timothy.dcrts.net.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import me.timothy.dcrts.net.LogicModule;
import me.timothy.dcrts.net.NetModule;

import org.yaml.snakeyaml.Yaml;

/**
 * Handles the basic modules, loading the modules, ect. Technically
 * a singleton, but can be used as if static
 * 
 * @author Timothy
 */
public class ModuleHandler {
	
	public static final ModuleHandler instance = new ModuleHandler();
	
	public static final String MODULE_FOLDER = "modules/"; 
	
	private static List<LogicModule> logModules;
	private static List<NetModule> netModules;
	
	static {
		netModules = new ArrayList<>();
		logModules = new ArrayList<>();
	}
	
	private ModuleHandler() {
	}

	/**
	 * Gets the net module associated with broadcasting.
	 * @return the broadcast module
	 */
	public static NetModule getBroadcastModule() {
		// Currently meant for the host peer after lobby turns into a connection state
		return getNetModuleByName("BroadcastModule");
	}

	/**
	 * Gets the module associated with purely listening. This should be a
	 * temporary measure to ensure synchronization.
	 * 
	 * @return the listener module
	 */
	public static NetModule getListenerModule() {
		// Meant for the other peers after the lobby turns into a connection state. Very temporary
		return getNetModuleByName("ListenerModule");
	}

	/**
	 * Gets an empty module. Should only be used as a temporary
	 * measure to ensure synchronization
	 * 
	 * @return an empty logic module
	 */
	public static LogicModule getEmptyModule() {
		// Meant for the other peers after the lobby turns into a connection state. Very temporary
		return getLogicModuleByName("EmptyModule");
	}
	
	public static LogicModule getLogicModuleByName(String name) {
		for(LogicModule lm : logModules) {
			if(lm.getName().equals(name)) {
				Class<? extends LogicModule> cl = lm.getClass();
				LogicModule module = null;
				try {
					module = cl.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
				return module;
			}
		}
		return null;
	}
	
	public static NetModule getNetModuleByName(String name) {
		for(NetModule nm : netModules) {
			if(nm.getName().equals(name)) {
				Class<? extends NetModule> cl = nm.getClass();
				NetModule module = null;
				try {
					module = cl.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
				return module;
			}
		}
		return null;
	}

	/**
	 * Loads a module into this module handlers memory
	 * @param url the url where it is located
	 */
	public static void loadModule(URL url) throws IOException {
		JarFile jarFile = new JarFile(url.getFile());
		
		ZipEntry descFile = jarFile.getEntry("description.yml");
		if(descFile == null) {
			jarFile.close();
			throw new IOException("Missing description file from jar at " + url);
		}
		
		InputStream inStream = jarFile.getInputStream(descFile);
		
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Object> objs = (Map<String, Object>) yaml.load(inStream);
		inStream.close();
		jarFile.close();
		
		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { url });
		String main = (String) objs.get("main");
		String type = (String) objs.get("type");
		String name = (String) objs.get("name");
		Class<?> cl = null;
		try {
			cl = urlClassLoader.loadClass(main);
		} catch (ClassNotFoundException e) {
			System.err.println("Misconfigured module (invalid main class) " + name);
			e.printStackTrace();
			return;
		}
		
		if(type.equals("logic")) {
			if(!LogicModule.class.isAssignableFrom(cl)) {
				System.err.println("Misconfigured module (main class does not subclass LogicModule) " + name);
				return;
			}

			LogicModule logModule = null;
			try {
				logModule = (LogicModule) cl.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				System.err.println("Misconfigured module (invalid constructor in main class) " + name);
				e.printStackTrace();
				return;
			}
			
			logModule.setName(name);
			logModules.add(logModule);
		}else if(type.equals("net")) {
			if(!NetModule.class.isAssignableFrom(cl)) {
				System.err.println("Misconfigured module (main class does not subclass NetModule) " + name);
				return;
			}

			NetModule netModule = null;
			try {
				netModule = (NetModule) cl.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				System.err.println("Misconfigured module (invalid constructor in main class) " + name);
				e.printStackTrace();
				return;
			}
			
			netModule.setName(name);
			netModules.add(netModule);
		}
		
		System.out.println("Successfully loaded " + name + " (" + cl.getCanonicalName() + ")");
	}

	public static File getFileForModule(String string) {
		return new File("modules/" + string + ".jar");
	}
}
