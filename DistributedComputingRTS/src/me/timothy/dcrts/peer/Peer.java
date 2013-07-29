package me.timothy.dcrts.peer;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents another real player in the game.
 * 
 * @author Timothy
 */
public abstract class Peer {
	
	protected String name;
	protected int id;
	protected boolean ready;
	/**
	 * Metadata about a peer. This is not saved
	 */
	public Map<String, Object> metaData;
	
	protected Peer() {
		metaData = new HashMap<>();
	}
	/**
	 * @return the name of the peer
	 */
	public String getName() {
		return name;
	}


	/**
	 * Set this peers name
	 * @param name the new name of this peer
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the id of this peer
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Sets the id of this peer
	 */
	public void setID(int id) {
		this.id = id;
	}
	
	/**
	 * if this peer to being ready for the game
	 * @return if this peer to being ready for the game
	 */
	public boolean isReady() {
		return ready;
	}
	
	/**
	 * Sets this peers readyness for the game
	 * @param ready the readyness
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		Peer other = (Peer) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
