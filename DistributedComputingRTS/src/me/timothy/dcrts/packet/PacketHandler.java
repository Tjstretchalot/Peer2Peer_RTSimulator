package me.timothy.dcrts.packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the method to be a packet handler. All packet handlers must
 * define both the correct header they handle, as well as there priority.
 * A higher priority means the packet will be called later, allowing you
 * to override the decisions of previous methods.
 * 
 * A priority of 1 is the lowest and 10 is the highest. 10 should only be used
 * by methods that do <i>not</i> affect the gamestate in any way (i.e. they are
 * simply monitoring the result). Use 3 if you are not sure
 * 
 * Specified methods must have two parameters - Peer and ParsedPacket
 * @author Timothy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketHandler {
	PacketHeader header();
	int priority();
}
