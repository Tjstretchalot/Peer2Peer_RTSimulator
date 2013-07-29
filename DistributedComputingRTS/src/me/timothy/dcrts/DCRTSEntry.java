package me.timothy.dcrts;

import java.util.logging.Level;
import java.util.logging.Logger;

import me.timothy.dcrts.state.ConnectionState;
import me.timothy.dcrts.state.LoadingState;
import me.timothy.dcrts.state.LobbyState;
import me.timothy.dcrts.state.MainMenuState;
import me.timothy.dcrts.state.MessageState;
import me.timothy.dcrts.utils.GUtils;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.GameState;
import org.newdawn.slick.state.StateBasedGame;

public class DCRTSEntry extends StateBasedGame {
	public static DCRTSEntry instance;
	
	public static GameState[] GAME_STATES = new GameState[] {
		new LoadingState(),
		new MainMenuState(),
		new LobbyState(),
		new MessageState(),
		new ConnectionState()
	};
	
	public DCRTSEntry() {
		super("Distributed Computing RTS");
	}

	@Override
	public void initStatesList(GameContainer cont) throws SlickException {
		GUtils.setWidth(getContainer().getWidth());
		GUtils.setHeight(getContainer().getHeight());
		
		for(GameState gs : GAME_STATES) {
			addState(gs);
		}
	}

	public static void main(String[] args) {
		try {
			AppGameContainer appgc;
			appgc = new AppGameContainer(instance = new DCRTSEntry());
			appgc.setDisplayMode(640, 480, false);
			appgc.setTargetFrameRate(120);
//			appgc.setAlwaysRender(true);
			appgc.setShowFPS(false);
			appgc.start();
		} catch (SlickException ex) {
			Logger.getLogger(DCRTSEntry.class.getName()).severe("Failed to start-up!");
			Logger.getLogger(DCRTSEntry.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
