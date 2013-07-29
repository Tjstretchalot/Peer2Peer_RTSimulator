package me.timothy.dcrts.net.lobby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.timothy.dcrts.net.ChatMessage;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.settings.GameSettings;

/**
 * Represents a lobby, which handles all of the related networking code.
 * Players who join the lobby will be kept in sync, and may speak in the
 * chatroom. All players have equivilent permissions
 * 
 * @author Timothy
 */
public abstract class Lobby {
	protected List<Peer> connectedPeers;
	protected Peer localPeer;
	protected List<ChatMessage> chatLog;
	
	public Lobby() {
	}
	
	/**
	 * Gets the number of peers currently in the lobby,
	 * not including the local peer
	 * @return number of connected peers
	 */
	public int numPeers() {
		return connectedPeers.size() + 1;
	}
	
	/**
	 * Gets all of the peers that are connected, excluding
	 * the local peer
	 * @return
	 */
	public List<Peer> getConnected() {
		return connectedPeers;
	}
	
	/**
	 * Get the local peer
	 * @return the local peer
	 */
	public Peer getLocalPeer() {
		return localPeer;
	}
	
	/**
	 * Change the local players name and sync
	 * @param newName the new name of the local player
	 */
	public abstract void changeName(String newName);
	
	/**
	 * Get the current game settings
	 * @return game settings
	 */
	public abstract GameSettings getSettings();
	
	/**
	 * Update the game settings and sync
	 * @param settings the new game settings
	 */
	public abstract void updateSettings(GameSettings settings);
	
	/**
	 * Set the local player to ready
	 * @param ready if the local player is ready or not
	 */
	public abstract void setReady(boolean ready);
	
	/**
	 * Begin the lobby
	 */
	public void begin() {
		connectedPeers = Collections.synchronizedList(new ArrayList<Peer>());
		chatLog = Collections.synchronizedList(new ArrayList<ChatMessage>());
	}
	
	/**
	 * Begin the countdown for the game
	 */
	public abstract void beginCountdown();
	
	/**
	 * Interrupt the countdown for starting the game. 
	 * THIS IS NOT THE SAME AS setReady(false)
	 */
	public abstract void interruptReady();
	
	protected Peer getPeerByID(int id) {
		synchronized(connectedPeers) {
			for(Peer peer : connectedPeers) {
				if(peer.getID() == id)
					return peer;
			}
		}
		return null;
	}
	/**
	 * 'Destroy' the connection and clean up memory.
	 */
	public void destroy() {
		connectedPeers = null;
		localPeer = null;
		chatLog = null;
	}
	
	/**
	 * Handles the specified chat, optionally sent by a particular peer.
	 * 
	 * @param peer The peer who sent the chat, may be null
	 * @param string the string to display
	 */
	protected void handleChat(Peer peer, String string) {
		System.out.println(string);
		
		chatLog.add(new ChatMessage(peer, string));
	}
	
	/**
	 * Get a direct list containing the chat log. Should not be modified,
	 * must be used as a synchronized list (synchronized blocks when being
	 * looped over, or when you need consistency across multiple calls).
	 * 
	 * @return The current chat log
	 */
	public List<ChatMessage> getChatLog() {
		return chatLog;
	}
	
	/**
	 * Ping the relevant peer
	 */
	public abstract void ping();
	
	public boolean isEveryoneReady() {
		if(!localPeer.isReady())
			return false;
		synchronized(connectedPeers) {
			for(Peer p : connectedPeers) {
				if(!p.isReady())
					return false;
			}
		}
		return true;
	}
}
