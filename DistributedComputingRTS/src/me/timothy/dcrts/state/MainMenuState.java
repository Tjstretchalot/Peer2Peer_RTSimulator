package me.timothy.dcrts.state;

import java.awt.Rectangle;
import java.net.InetSocketAddress;

import me.timothy.dcrts.DCRTSEntry;
import me.timothy.dcrts.utils.GUtils;
import me.timothy.dcrts.utils.NetUtils;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

public class MainMenuState extends BasicGameState {
	public static final int ID = 1;
	private static final String[] BUTTON_STRINGS = new String[] {
		"Create Lobby",
		"Join Lobby"
	};
	
	private Rectangle[] buttonLocs;
	
	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		buttonLocs = new Rectangle[BUTTON_STRINGS.length];
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics graphics)
			throws SlickException {
		int y = 160;
		for(int i = 0; i < BUTTON_STRINGS.length; i++) {
			if(buttonLocs[i] == null) {
				buttonLocs[i] = new Rectangle(0, 0, 0, 0);
			}
			GUtils.drawCenteredX(graphics, BUTTON_STRINGS[i], buttonLocs[i], y);
			GUtils.drawHoverEffect(graphics, BUTTON_STRINGS[i], buttonLocs[i]);
			y += 50;
		}
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		for(int i = 0; i < buttonLocs.length; i++) {
			if(buttonLocs[i] == null)
				continue;
			
			if(GUtils.mouseInside(buttonLocs[i]) && Mouse.isButtonDown(0)) {
				onPress(i);
			}
		}
	}

	private void onPress(int i) {
		switch(BUTTON_STRINGS[i]) {
		case "Create Lobby":
			((LobbyState) DCRTSEntry.GAME_STATES[LobbyState.ID]).createNew(true);
			DCRTSEntry.instance.enterState(LobbyState.ID, new FadeOutTransition(), new FadeInTransition());
			break;
		case "Join Lobby":
			LobbyState lobbyState = (LobbyState) DCRTSEntry.GAME_STATES[LobbyState.ID];
			lobbyState.createNew(false);
			lobbyState.join(new InetSocketAddress("localhost", NetUtils.PORT));
			DCRTSEntry.instance.enterState(LobbyState.ID, new FadeOutTransition(), new FadeInTransition());
			break;
		default:
			throw new RuntimeException("Can't get here!");
		}
	}

	@Override
	public int getID() {
		return ID;
	}

	

}
