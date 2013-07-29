package me.timothy.dcrts.state;

import me.timothy.dcrts.DCRTSEntry;
import me.timothy.dcrts.utils.GUtils;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;
import org.newdawn.slick.state.transition.Transition;

public class MessageState extends BasicGameState {
	public static final int ID = 3;
	
	private String message;
	private int initialTimeSeconds;
	private long timeLeft;
	private int nextId;
	private Transition transitionOut;
	private Transition transitionIn;
	
	public MessageState() {
		
	}
	
	/**
	 * Prepares this message state for being entered into
	 * @param message The message to display
 	 * @param timeSeconds the time, in seconds, to display it
	 * @param nextId the next id to transition to
	 * @param transOut the transition to use when leaving this state
	 * @param transIn the transition to use when enterring the next state
	 */
	public void prepare(String message, int timeSeconds, int nextId, Transition transOut, Transition transIn) {
		System.out.println(message);
		this.message = message;
		this.initialTimeSeconds = timeSeconds;
		this.timeLeft = initialTimeSeconds * 1000;
		this.nextId = nextId;
		this.transitionOut = transOut;
		this.transitionIn = transIn;
	}
	
	/**
	 * Prepares this message state for being entered into, with a fade
	 * transition
	 * 
	 * @param message the message to display
	 * @param timeSeconds the time in seconds to display it
	 * @param nextId the id to transition to
	 */
	public void prepare(String message, int timeSeconds, int nextId) {
		prepare(message, timeSeconds, nextId, new FadeOutTransition(), new FadeInTransition());
	}
	
	@Override
	public void init(GameContainer cont, StateBasedGame game)
			throws SlickException {
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics graphics)
			throws SlickException {
		GUtils.drawCenteredX(graphics, message, null, GUtils.height / 2 - graphics.getFont().getHeight(message) / 2);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		timeLeft -= delta;
		
		if(timeLeft < 0) {
			DCRTSEntry.instance.enterState(nextId, transitionOut, transitionIn);
		}
	}

	@Override
	public int getID() {
		return ID;
	}

}
