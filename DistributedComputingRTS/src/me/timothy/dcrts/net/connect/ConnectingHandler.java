package me.timothy.dcrts.net.connect;

import me.timothy.dcrts.GameState;
import me.timothy.dcrts.net.LogicModule;
import me.timothy.dcrts.net.NetModule;
import me.timothy.dcrts.net.NetState;

/**
 * The connection handler is the sole handler for
 * when a lobby begins connecting to each other,
 * but prior to the game actually starting.
 * 
 * The connecting handler is led by two modules,
 * which do *not* have a dependency on the handler,
 * a NetModule and a LogicModule.
 * 
 * The connecting handler will not handle the threading,
 * so the modules must handle synchronization appropriately.
 * Creating the initial modules is part of the ConnectingBuilder.
 * 
 * @author Timothy
 * @see me.timothy.dcrts.net.Module
 * @see me.timothy.dcrts.net.NetModule
 * @see me.timothy.dcrts.net.LogicModule
 * @see me.timothy.dcrts.net.connect.ConnectingBuilder
 */
public class ConnectingHandler {
	private NetModule netModule;
	private LogicModule logicModule;
	
	private NetState netState;
	private GameState gameState;
	
	public ConnectingHandler(NetModule nm, LogicModule lm, NetState ns, GameState gs) {
		netModule = nm;
		logicModule = lm;
		netState = ns;
		gameState = gs;
	}
	
	public void begin() {
		netModule.onActivate();
		logicModule.onActivate();
	}
	
	public NetState getNetState() {
		return netState;
	}
	
	public GameState getGameState() {
		return gameState;
	}

	public void destroy() {
		netModule.onDeactivate();
		logicModule.onDeactivate();
	}
}
