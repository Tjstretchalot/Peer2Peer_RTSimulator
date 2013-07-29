package me.timothy.dcrts.state;

import java.awt.Rectangle;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import me.timothy.dcrts.DCRTSEntry;
import me.timothy.dcrts.net.PingingThread;
import me.timothy.dcrts.net.lobby.ConnectedLobby;
import me.timothy.dcrts.net.lobby.HostedLobby;
import me.timothy.dcrts.net.lobby.Lobby;
import me.timothy.dcrts.net.lobby.LobbyPackets;
import me.timothy.dcrts.net.packets.CountdownChangedPacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.GUtils;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

public class LobbyState extends BasicGameState implements PacketListener {
	public static final int ID = 2;
	
	private Lobby lobby;
	private long lastPing;
	private long lastButtonPress;
	private boolean pinging;
	private int countdown;
	private PingingThread pingingThread;
	
	public LobbyState() {
	}
	
	private static Rectangle[] BUTTON_LOCS = new Rectangle[] {
		new Rectangle(-55, -50, 1, 1),
		new Rectangle(-50, -25, 1, 1),
		new Rectangle(10, -25, 1, 1)
	};
	
	private static String[] BUTTON_TEXT = new String[] {
		"Ready",
		"Ping",
		"Return"
	};
	
	private List<Rectangle> playerNameRects;
	
	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		LobbyPackets.init();
		PacketManager.instance.registerClass(this);
		playerNameRects = new ArrayList<>();
		playerNameRects = Collections.synchronizedList(playerNameRects);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		if(lobby != null) {
			GUtils.draw(g, "Number of Players: " + lobby.numPeers(), null, 5, 5);
			if(!pinging)
				GUtils.draw(g, "Ping: " + lastPing, null, 5, 20);
			else
				GUtils.draw(g, "Pinging...", null, 5, 20);
		}
		boolean foundNonReady = false;
		if(lobby != null) {
			foundNonReady = !lobby.isEveryoneReady();
			if(!foundNonReady) {
				lobby.beginCountdown();
				GUtils.drawCenteredX(g, Integer.toString(countdown), null, 240 - g.getFont().getHeight(Integer.toString(countdown)));
			}

		}
		
		for(int i = 0; i < BUTTON_LOCS.length; i++) {
			Rectangle rect = BUTTON_LOCS[i];
			String text = BUTTON_TEXT[i];
			
			GUtils.draw(g, text, rect, rect.x, rect.y);
			GUtils.drawHoverEffect(g, text, rect);
		}
		synchronized(playerNameRects) {
			int x = 75;
			int y = 55;
			if(lobby != null) {
				String localPlayerName = lobby.getLocalPeer() != null ? lobby.getLocalPeer().getName() : "null";

				if(playerNameRects.size() < 1)
					playerNameRects.add(new Rectangle(1, 1));
				GUtils.draw(g, localPlayerName, playerNameRects.get(0), x, y);
				GUtils.drawHoverEffect(g, localPlayerName, playerNameRects.get(0));
				if(lobby.getLocalPeer().isReady()) {
					g.drawString("R", (float) playerNameRects.get(0).getMaxX() + 5, (float) y);
				}

				List<Peer> connected = lobby.getConnected();
				synchronized(connected) {
					for(int i = 0; i < connected.size(); i++) {
						y += 30;
						Peer p = connected.get(i);
						String name = p != null ? p.getName() : "null";
						if(playerNameRects.size() < i + 2)
							playerNameRects.add(new Rectangle(1, 1));
						GUtils.draw(g, name, playerNameRects.get(i + 1), x, y);
						if(p.isReady()) {
							g.drawString("R", (float) playerNameRects.get(i + 1).getMaxX() + 5, (float) y);
						}
					}

				}
			}
		}
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		if(!Mouse.isButtonDown(0))
			return;
		if(System.currentTimeMillis() - lastButtonPress < 500)
			return;
		for(int i = 0; i < BUTTON_LOCS.length; i++) {
			Rectangle rect = BUTTON_LOCS[i];
			
			if(GUtils.mouseInside(rect)) {
				onPress(i);
				lastButtonPress = System.currentTimeMillis();
			}
		}
		
		for(int i = 0; i < playerNameRects.size(); i++) {
			Rectangle rect = playerNameRects.get(i);
			
			if(GUtils.mouseInside(rect)) {
				onPlayerPress(i);
				lastButtonPress = System.currentTimeMillis();
			}
		}
	}

	@Override
	public int getID() {
		return ID;
	}
	
	public void createNew(boolean hosting) {
		if(hosting) {
			lobby = new HostedLobby();
			lobby.begin();
		}else {
			lobby = new ConnectedLobby();
		}
	}
	
	public void join(SocketAddress addr) {
		((ConnectedLobby) lobby).setAddress(addr);
		lobby.begin();
	}
	
	protected void onPress(int i) {
		String text = BUTTON_TEXT[i];
		
		switch(text) {
		case "Ready":
			if(lobby.isEveryoneReady())
				lobby.interruptReady();
			else
				lobby.setReady(!lobby.getLocalPeer().isReady());
			break;
		case "Return":
			if(lobby != null) {
				if(lobby instanceof HostedLobby)
					((HostedLobby) lobby).shutdownServer();
				lobby.destroy();
				lobby = null;
			}
			
			DCRTSEntry.instance.enterState(MainMenuState.ID, new FadeOutTransition(), new FadeInTransition());
			break;
		case "Ping":
			if(pinging || lobby instanceof HostedLobby)
				return;
			pinging = true;
			pingingThread = new PingingThread(this, lobby);
			PacketManager.instance.registerClass(pingingThread);
			pingingThread.start();
			break;
		default:
			System.err.println("Nothing configured for that button (" + text + ")");
			break;
		}
	}
	
	protected void onPlayerPress(int i) {
		String newName = JOptionPane.showInputDialog("What would you like to change your name to?", lobby.getLocalPeer().getName());
		lobby.changeName(newName);
	}
	
	public void setPingTime(long time) {
		lastPing = time;
		pinging = false;
		PacketManager.instance.unregisterClass(pingingThread);
		pingingThread = null;
	}
	
	@PacketHandler(header=PacketHeader.SYNC_COUNTDOWN, priority=3)
	public void syncCountdown(Peer peer, ParsedPacket packet) {
		CountdownChangedPacket scp = (CountdownChangedPacket) packet;
		countdown = scp.getCountdown();
	}
}
