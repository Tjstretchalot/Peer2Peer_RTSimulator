package me.timothy.dcrts;

import java.util.List;

import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.settings.GameSettings;

/**
 * Describes a game state, including the connected peers.
 * 
 * @author Timothy
 */
public class GameState {
	private Peer local;
	private List<Peer> connectedPeers; // ListenerModule depends on index 0 being original host
	private GameSettings settings;
	
	/**
	 * @param local 
	 * @param peers should be a synchronized list
	 * @param settings 
	 */
	public GameState(Peer local, List<Peer> peers, GameSettings settings) {
		if(!peers.getClass().getName().toLowerCase().contains("sync"))
			throw new RuntimeException("Not a synchronized list!");
		
		this.local = local;
		connectedPeers = peers;
		this.settings = settings;
	}
	
	public Peer getLocalPeer() {
		return local;
	}
	
	public GameSettings getSettings() {
		return settings;
	}
	
	/**
	 * Returns a synchronized list of peers. As per synchronization
	 * rules, when iterating over the list it must be inside
	 * a synchronized block.
	 * @return the list of connected peers
	 */
	public List<Peer> getConnectedPeers() {
		return connectedPeers;
	}
}
