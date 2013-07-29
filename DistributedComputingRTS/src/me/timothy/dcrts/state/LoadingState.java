package me.timothy.dcrts.state;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import me.timothy.dcrts.DCRTSEntry;
import me.timothy.dcrts.ResourceManager;
import me.timothy.dcrts.net.module.ModuleHandler;
import me.timothy.dcrts.utils.GUtils;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

public class LoadingState extends BasicGameState {
	public static final int ID = 0;
	
	private File[] toLoad;
	private List<String> resourcesToLoad;
	
	private int index;
	private int maxIndex;
	private boolean modules;
	
	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		index = 0;
		toLoad = new File(ModuleHandler.MODULE_FOLDER).listFiles();
		maxIndex = toLoad.length;
		System.out.println("Modules: "+  Arrays.deepToString(toLoad));
		modules = true;
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		if(toLoad != null && toLoad.length > 0)
			GUtils.drawCenteredX(g, "Loading... " + ((index / (double) maxIndex) * 100) + "%",
					null, container.getHeight() / 2 - g.getFont().getLineHeight() / 2);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		if(modules) {
			File f = toLoad[index];
			index++;
			if(!f.getName().endsWith(".jar")) {
				return;
			}

			try {
				ModuleHandler.loadModule(f.toURI().toURL());
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(index == toLoad.length) {
				System.out.println("Finished loading modules");
				modules = false;
				try {
					resourcesToLoad = ResourceManager.getAllResourceFiles();
				} catch (IOException e) {
					e.printStackTrace();
				}
				index = 0;
				maxIndex = resourcesToLoad.size();
				System.out.println("Loading resources...");
			}
		}else {
			String str = resourcesToLoad.get(index);
			index++;
			try {
				ResourceManager.loadResource(str);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(index == maxIndex) {
				System.out.println("Finished loading resources");
				DCRTSEntry.instance.enterState(MainMenuState.ID, new FadeOutTransition(), new FadeInTransition());
			}
		}
	}

	@Override
	public int getID() {
		return ID;
	}

}
