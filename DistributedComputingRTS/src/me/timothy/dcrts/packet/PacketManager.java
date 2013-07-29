package me.timothy.dcrts.packet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.ErrorUtils;

/**
 * When the server runs, they should get one
 * instance of a packet-manager to allow them
 * to properly parse messages (via PacketParser)
 * call parsed packets (packetlisteners with 
 * methods containing a packethandler annotation),
 * and to send out parsed packets (outgoingpacketlistener,
 * outgoingpackethandler).
 * 
 * In order to be a packet parsing method, you must have a 
 * function (name-irrelevant) with the following parameters 
 * and return type
 * 
 * <pre>
 * {@code
 *   public ParsedPacket foo(PacketHeader header, ByteBuffer buffer);
 * }
 * </pre>
 * 
 * Inside of that method you <i>must</i> use a synchronized block (on the buffer)
 * from your first method call on the buffer to the end, like so:
 * 
 * <pre>
 * {@code
 *   public ParsedPacket foo(PacketHeader header, ByteBuffer buffer) {
 *     synchronized(buffer) {
 *       int rem = buffer.remaining();
 *       // ...
 *     }
 *   }
 * }
 * </pre>
 * 
 * At the end of the buffer, you must of used the entire buffer relevant to
 * your packet, such that the buffer either has no bytes remaining, or the
 * next 4 bytes contain the header for the next packet. EG:
 * <pre>
 * PRECONDITION: BUFFER: Pos=0, Limit=32
 * POSTCONDITION: BUFFER: Pos=32, Limit=32
 * </pre>
 * or alternatively, if there are two 32byte packets in the buffer
 * <pre>
 * PRECONDITION: Buffer: Pos=32, Limit=96
 * POSTCONDITION: Buffer: Pos=64, Limit=96
 * </pre>
 * The buffer <i>must not be</i> flipped.
 * 
 * Example for how to register an instance and function name as a packet parser:
 * <pre>
 * {@code
 *   void register(PacketManager manager, String functionName, PacketHeader header) {
 *     manager.registerPacketParser(this, functionName, header);
 *   }
 * }</pre>
 * 
 * PacketListeners may register there entire class at once, or one method at a time. However,
 * they must not do both, to avoid registering methods multiple times. To declare a method as
 * a packet handler, use the <code>@PacketHandler</code> annotation, declaring which header
 * and priority the handler listens for. See PacketHandler for further explanation.<br>
 * PacketHandlers should accept exactly two parameters, Peer and ParsedPacket
 * 
 * Example:
 * <pre><code>
 * public class Bar implements PacketListener {
 *  &nbsp;@PacketHandler(header=Packet.CONNECT, priority=100)
 *   public void foo(Peer peer, ParsedPacket packet) {
 *   
 *   }
 * }
 * </code></pre>
 * and to register that class
 * <pre><code>
 * // packetManager is declared and initialized, not null, as a PacketManager
 * Bar bar = new Bar();
 * packetManager.registerClass(bar);
 * </pre></code>
 * or alternatively, to just register that method
 * <pre><code>
 * // packetManager is declared and initialized, not null, as a PacketManager
 * Bar bar = new Bar();
 * packetManager.register(bar, "foo");
 * </pre></code>
 * @author Timothy
 *
 */
public class PacketManager {
	
	private class PacketParser {
		Object object;
		Map<Method, PacketHeader> methods;
		
		public PacketParser(Object obj) {
			methods = new HashMap<>();
			object = obj;
		}
		
		/*
		 * METHODS ARE IGNORED IN hashCode() AND equals
		 */

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((object == null) ? 0 : object.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PacketParser other = (PacketParser) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (object == null) {
				if (other.object != null)
					return false;
			} else if (!object.equals(other.object))
				return false;
			return true;
		}

		private PacketManager getOuterType() {
			return PacketManager.this;
		}
	}
	
	private class PacketSender {
		Object object;
		Map<Method, PacketHeader> methods;
		
		public PacketSender(Object obj) {
			methods = new HashMap<>();
			object = obj;
		}

		/*
		 * METHODS ARE IGNORED IN hashCode() AND equals
		 */
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((object == null) ? 0 : object.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PacketSender other = (PacketSender) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (object == null) {
				if (other.object != null)
					return false;
			} else if (!object.equals(other.object))
				return false;
			return true;
		}

		private PacketManager getOuterType() {
			return PacketManager.this;
		}
	}

	public static final PacketManager instance = new PacketManager();
	
	private List<PacketParser> packetParsers;
	private List<PacketSender> packetSenders;
	private Map<PacketHeader, Map<Method, Object>> packetListeners;
	
	public PacketManager() {
		packetParsers = new ArrayList<>();
		packetSenders = new ArrayList<>();
		packetListeners = new HashMap<>();
	}
	
	/**
	 * Register an object and function as the only parser for the specified packet header.
	 * The function must accept two parameters, PacketHeader and ByteBuffer. Should
	 * return a ParsedPacket
	 *  
	 * @param obj the object to register with
	 * @param functionName the function in question, that parses all incoming buffers with the {@code header} header.
	 * @param header The header that is being parsed.
	 * 
	 * @throws IllegalArgumentException if there is already a packet parser for that header, or if
	 *   the function does not have valid parameter types or return type.
	 * @throws NullPointerException if any of the parameters are null
	 */
	public void registerPacketParser(Object obj, String functionName, PacketHeader header) throws
	  IllegalArgumentException, NullPointerException {
		if(obj == null || functionName == null || header == null) {
			ErrorUtils.nullPointer(new String[] { "obj", "functionName", "header" }, obj, functionName, header);
		}
		
		PacketParser pParser = new PacketParser(obj);
		if(packetParsers.contains(pParser)) {
			pParser = packetParsers.get(packetParsers.indexOf(pParser));
		}else {
			packetParsers.add(pParser);
		}

		Method method;
		Class<?> cl = obj.getClass();
		try {
			method = cl.getMethod(functionName, PacketHeader.class, ByteBuffer.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException(e);
		}
		
		if(pParser.methods.containsKey(method)) {
			System.err.println("Warning - " + obj.getClass().getName() + "'s \"" + functionName + "\" was registered twice");
			return;
		}
		
		pParser.methods.put(method, header);
	}
	
	/**
	 * Register an object and function pair as the <i>sole</i> method used for sending
	 * packets of that particular type. This method will be called whenever a packet
	 * of that type needs to be sent, and should accept an object array as the last parameter.
	 * EG:
	 * <pre>{@code
	 *   public void pingSender(ByteBuffer buffer, Object... args);
	 * }</pre>
	 * 
	 * Where buffer is the writable buffer, and args are the relevant arguments
	 * 
	 * @param obj the object to call the method on. 
	 * @param functionName the function
	 * @param header the header to send packets for
	 */
	public void registerPacketSender(Object obj, String functionName, PacketHeader header) throws
	  IllegalArgumentException, NullPointerException {
		if(obj == null || functionName == null || header == null) {
			ErrorUtils.nullPointer(new String[] { "obj", "functionName", "header" }, obj, functionName, header);
		}
		
		PacketSender pSender = new PacketSender(obj);
		int ind = packetSenders.indexOf(pSender);
		if(ind != -1) {
			pSender = packetSenders.get(ind);
		}else {
			packetSenders.add(pSender);
		}
		
		Method method;
		Class<?> cl = obj.getClass();
		try {
			method = cl.getMethod(functionName, ByteBuffer.class, Object[].class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException(e);
		}
		
		if(pSender.methods.containsKey(method)) {
			System.err.println("Warning - " + obj.getClass().getName() + "'s \"" + functionName + "\" was registered twice");
			System.err.println("Besides rethinking how the code works, this message can be safely ignored");
			return;
		}
		
		pSender.methods.put(method, header);
	}
	
	/**
	 * Registers the specified method to recieve packet
	 * events.
	 * @param obj the object to invoke the method on
	 * @param method the method to register
	 */
	public void register(Object obj, Method method) {
		if(!method.isAnnotationPresent(PacketHandler.class))
			throw new IllegalArgumentException("Method does not have packethandler annotation");
		PacketHandler pHandler = method.getAnnotation(PacketHandler.class);
		if(!packetListeners.containsKey(pHandler.header())) {
			packetListeners.put(pHandler.header(), new HashMap<Method, Object>());
		}
		packetListeners.get(pHandler.header()).put(method, obj);
	}
	
	/**
	 * Registers the specified function from the specified object.
	 * 
	 * @param obj The object to get the method from
	 * @param functionName the name of the method
	 */
	public void register(PacketListener obj, String functionName) {
		try {
			Method method = obj.getClass().getDeclaredMethod(functionName, ParsedPacket.class);
			register(obj, method);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Registers all declared methods from the specified object with the
	 * PacketHandler annotation.
	 * 
	 * @param obj The object to register all PacketHandler methods from
	 */
	public void registerClass(PacketListener obj) {
		Method[] methods = obj.getClass().getDeclaredMethods();
		for(Method method : methods) {
			if(method.isAnnotationPresent(PacketHandler.class)) {
				if(!method.isAccessible()) {
					method.setAccessible(true);
				}
				register(obj, method);
			}
		}
	}
	
	/**
	 * Parses the specified header and buffer based on the registered packet parser
	 * @param header the header of the buffer
	 * @param buffer the buffer itself
	 * @return the parsed packet, based on the registered packet parser
	 */
	public ParsedPacket parse(PacketHeader header, ByteBuffer buffer) {
		if(header == null || buffer == null)
			ErrorUtils.nullPointer(new String[] { "header", "buffer" }, header, buffer);
		
		Object[] arr = getParserRegisteredFor(header);
		
		if(arr == null) {
			throw new IllegalArgumentException("No sender registered for " + header.name() + "!");
		}
		
		PacketParser parser = (PacketParser) arr[0];
		Method method = (Method) arr[1];
		
		if(parser.object == null || header == null || buffer == null || method == null) {
			ErrorUtils.nullPointer(new String[] {
					"method", "parser.object",
					"header", "buffer"
			}, method, parser.object, header, buffer);
		}
		
		try {
			return (ParsedPacket) method.invoke(parser.object, header, buffer);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.err.println("Parser invalidly registered for " 
			+ parser.object.getClass().getName() + "#" + method.getName());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Writes a packet for the specified header into the specified buffer.
	 * The buffer <b>must</b> already have the peer id it started from in it
	 * 
	 * @param header the header
	 * @param buffer the buffer to write into
	 * @param args the arguments to pass on
	 */
	public void send(PacketHeader header, ByteBuffer buffer, Object... args) {
		Object[] arr = getSenderRegisteredFor(header);
		
		if(arr == null) {
			throw new IllegalArgumentException("No sender registered for " + header.name() + "!");
		}
		
		PacketSender sender = (PacketSender) arr[0];
		Method method = (Method) arr[1];
		
		if(sender.object == null || header == null || args == null || method == null || buffer == null) {
			ErrorUtils.nullPointer(new String[] {
					"method", "sender.object",
					"header", "buffer", "args"
			}, method, sender.object, header, buffer, args);
		}
		try {
			method.invoke(sender.object, buffer, args);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.err.println("Sender invalidly registered for " 
			+ sender.object.getClass().getName() + "#" + method.getName());
		}
	}

	/**
	 * returns the parser registered for the specified header
	 * @param header the header to get the parser for
	 * @return the parser for that header, or null if none is registered
	 */
	public Object[] getParserRegisteredFor(PacketHeader header) {
		for(PacketParser parser : packetParsers) {
			Set<Method> keySet = parser.methods.keySet();
			for(Method method : keySet) {
				PacketHeader registered = parser.methods.get(method);
				if(registered == header)
					return new Object[] { parser, method };
			}
		}
		return null;
	}
	
	/**
	 * Retrieves and returns the packet sender registered for the specified header
	 * @param header the header
	 * @return the associated sender
	 */
	public Object[] getSenderRegisteredFor(PacketHeader header) {
		for(PacketSender sender : packetSenders) {
			Set<Method> keySet = sender.methods.keySet();
			for(Method method : keySet) {
				PacketHeader registered = sender.methods.get(method);
				if(registered == header)
					return new Object[] { sender, method };
			}
		}
		return null;
	}
	
	/**
	 * Broadcasts the specified parsed packet to all of the
	 * registered listeners for the header of {@code packet}
	 * @param packet the packet to broadcast
	 */
	public void broadcastPacket(Peer peer, ParsedPacket packet) {
		Map<Method, Object> registered = getRegistered(packet.getHeader());
		
		if(registered == null || registered.size() == 0) {
			System.err.println("Packet broadcast with no listeners: " + packet.getHeader().name());
			return;
		}
		
		Set<Method> keySet = registered.keySet();
		for(Method method : keySet) {
			if(method == null)
				return;
			try {
				method.invoke(registered.get(method), peer, packet);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new RuntimeException("Misconfigured method registered! (" + 
					(registered.containsKey(method) ? registered.get(method).getClass().getName() : "unknown class") 
					+ ")", e);
			}
		}
	}

	private Map<Method, Object> getRegistered(PacketHeader header) {
		Map<Method, Object> result = packetListeners.get(header);
		if(result == null)
			result = new HashMap<>();
		if(packetListeners.containsKey(PacketHeader.ANY))
			result.putAll(packetListeners.get(PacketHeader.ANY));
		return result;
	}

	/**
	 * Only way to unregister a packet listener is via the entire class.
	 * @param listener the listener to unregister
	 */
	public void unregisterClass(PacketListener listener) {
		Set<PacketHeader> keySet1 = packetListeners.keySet();
		Map<PacketHeader, Method> toRemove = new HashMap<>();
		for(PacketHeader pHeader : keySet1) {
			Map<Method, Object> methodsForType = packetListeners.get(pHeader);
			Set<Method> keySet2 = methodsForType.keySet();
			
			for(Method method : keySet2) {
				Object obj = methodsForType.get(method);
				if(obj == listener) {
					toRemove.put(pHeader, method);
				}
			}
		}
		
		Set<PacketHeader> keySet3 = toRemove.keySet();
		for(PacketHeader pHeader : keySet3) {
			packetListeners.get(pHeader).remove(toRemove.get(pHeader));
		}
	}
}
