package me.timothy.dcrts.net;

import me.timothy.dcrts.GameState;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;

public abstract class Module implements PacketListener {
	protected PacketManager pManager;
	protected NetState netState;
	protected GameState gameState;
	protected String name;
	
	protected Module() {
		setName(getClass().getSimpleName());
	}
	/**
	 * Called when a module is activated. This may be
	 * called multiple times, but it will always be 
	 * paired with onDeactivate. This is called
	 * immediately following setResources
	 */
	public abstract void onActivate();
	
	/**
	 * Called when a module is no longer active. This may be
	 * called multiple times, but it will always be paired with
	 * onActivate. This is called immediately prior to nullResources
	 */
	public abstract void onDeactivate();
	
	/**
	 * Nulls out packet manager, net state and game state.
	 */
	public final void nullResources() {
		pManager = null;
		netState = null;
		gameState = null;
	}
	
	/**
	 * Sets resources
	 * @param pm the packet manager
	 * @param ns the current net state
	 * @param gs the game state
	 */
	public final void setResources(PacketManager pm, NetState ns, GameState gs) {
		pManager = pm;
		netState = ns;
		gameState = gs;
	}
	
	/**
	 * Retrieves this modules name
	 * @return the name of the module
	 */
	public final String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the module
	 * @param str the new name
	 */
	public final void setName(String str) {
		name = str;
	}
}
