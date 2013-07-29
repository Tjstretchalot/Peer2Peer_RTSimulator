package me.timothy.dcrts.net.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages recieving and sending client-related events.
 * 
 * @author Timothy
 */
public class CEventManager {
	private static class EventReciever {
		public Object obj;
		public List<Method> methods;
		
		public EventReciever(Object o) {
			obj = o;
			methods = new ArrayList<>();
		}
		
		public void call(EventType type, Object... args) {
			for(Method m : methods) {
				try {
					if(m.getAnnotation(CEventHandler.class).event() == type)
						m.invoke(obj, args);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					System.err.println("Method " + m.getName() 
							+ " is improperly registered and threw an error! (Class: "
							+ m.getClass().getCanonicalName() + ")");
					e.printStackTrace();
				}
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
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
			EventReciever other = (EventReciever) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			return true;
		}
	}
	
	public static final CEventManager instance = new CEventManager();
	
	private List<EventReciever> recievers;
	
	public CEventManager() {
		recievers = new ArrayList<>();
	}
	
	/**
	 * Registers the specified object to recieve events based on
	 * method annotations.
	 * 
	 * @param obj the object to register
	 */
	public void register(Object obj) {
		Method[] methods = obj.getClass().getDeclaredMethods();
		
		EventReciever reciever = new EventReciever(obj);
		if(recievers.contains(reciever)) {
			System.err.println("Event Handler already registered! (" + obj.getClass().getCanonicalName() + ")");
			return;
		}
		
		for(Method method : methods) {
			if(method.isAnnotationPresent(CEventHandler.class))
				reciever.methods.add(method);
		}
		recievers.add(reciever);
	}
	
	/**
	 * Unregisters the specified object from recieving events.
	 * 
	 * @param obj The object to unregister
	 */
	public void unregister(Object obj) {
		EventReciever rec = new EventReciever(obj);
		if(!recievers.remove(rec))
			System.err.println("Remove was called on an unregistered object (" + obj.getClass().getCanonicalName() + ")");
	}
	
	/**
	 * Broadcasts the specified events to all listeners
	 * 
	 * @param type the event type
	 * @param args the arguments to pass on
	 */
	public void broadcast(EventType type, Object... args) {
		List<EventReciever> copy = new ArrayList<>();
		for(EventReciever rec : recievers) {
			copy.add(rec);
		}
		for(EventReciever reciever : copy) {
			reciever.call(type, args);
		}
	}
}
