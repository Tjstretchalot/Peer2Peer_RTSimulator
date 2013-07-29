package me.timothy.dcrts.net.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a method that recieves ClientEvents.
 * 
 * Methods recieving ClientEvents should have one parameter, either
 * ClientEvent or the appropriate sub-class for what they are calling
 * 
 * @author Timothy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CEventHandler {
	public EventType event();
}
