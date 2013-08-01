package me.timothy.dcrts.state;

import java.awt.Point;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import me.timothy.dcrts.GameState;
import me.timothy.dcrts.graphics.DropdownMenu;
import me.timothy.dcrts.graphics.EnableChecker;
import me.timothy.dcrts.net.LogicModule;
import me.timothy.dcrts.net.NetModule;
import me.timothy.dcrts.net.NetState;
import me.timothy.dcrts.net.connect.ConnectingHandler;
import me.timothy.dcrts.net.connect.ConnectionPackets;
import me.timothy.dcrts.net.event.CEventHandler;
import me.timothy.dcrts.net.event.CEventManager;
import me.timothy.dcrts.net.event.EventType;
import me.timothy.dcrts.net.module.ModuleHandler;
import me.timothy.dcrts.net.packets.DirectConnectionPacket;
import me.timothy.dcrts.net.packets.WhisperPacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.settings.GameSettings;
import me.timothy.dcrts.utils.GUtils;
import me.timothy.dcrts.utils.NetUtils;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

public class ConnectionState extends BasicGameState implements PacketListener {
	/**
	 * Points should be based on a circle, R=120
	 * @author Timothy
	 */
	protected class GraphPoint implements EnableChecker {
		protected Peer thePeer;
		protected Point point;
		protected List<GraphPoint> connected;
		
		protected float textX;
		protected float textY;
		
		GraphPoint(Peer peer, Point p) {
			thePeer = peer;
			point = p;
			connected = new ArrayList<>();
			textX = -1;
			textY = -1;
		}
		
		void render(GameContainer cont, StateBasedGame game, Graphics graphics, int xShift, int yShift) {
			graphics.fillOval(point.x + xShift, point.y + yShift, 8, 8);
			
			Color old = graphics.getColor();
			graphics.setColor(Color.lightGray);
			for(GraphPoint p : connected) {
				graphics.drawLine(point.x + xShift + 4f, point.y + yShift + 4f, p.point.x + xShift + 4f, p.point.y + yShift + 4f);
			}
			graphics.setColor(old);
		}
		
		void renderText(GameContainer cont, StateBasedGame game, Graphics graphics, int xShift, int yShift) {
			if(textX == -1 && textY == -1) {
				float widthName = graphics.getFont().getWidth(thePeer.getName());
				textX  = point.x + 4f - (widthName / 2);
				textY = point.y - 25;
			}
			graphics.drawString(thePeer.getName(), xShift + textX, yShift + textY);
		}
		
		void addConnectedPoint(GraphPoint graphPoint) {
			connected.add(graphPoint);
		}

		public boolean isBeingHoveredOn(int xShift, int yShift) {
			int xDis = Math.abs((point.x + xShift) - Mouse.getX());
			int yDis = Math.abs((point.y + yShift) - (GUtils.height - Mouse.getY()));
			/*
			System.out.println("point: (" + point.x + ", " + point.y + ")");
			System.out.println("shift: (" + xShift + ", " + yShift + ")");
			System.out.println("shifted: (" + (point.x + xShift) + ", " + (point.y + yShift) + ")");
			System.out.println();
			System.out.println("mouse: (" + Mouse.getX() + ", " + Mouse.getY() + ")");
			System.out.println("dist: (" + xDis + ", " + yDis + ")");*/
			double dis = Math.sqrt(xDis * xDis + yDis * yDis);
			//System.out.println("    = " + dis);
			if(dis > 8) { // wiggle room
				return false;
			}
			return true;
		}
		
		@Override
		public boolean isEnabled(Object obj, Map<String, Object> meta) {
			if(obj instanceof String)
				return isEnabled((String) obj, meta);
			if(obj instanceof List<?>) {
				String str = (String) ((List<?>) obj).get(0);
				return isEnabled(str, meta);
			}
			return false;
		}
		
		
		public boolean isEnabled(String str, Map<String, Object> meta) {
			switch(str) {
			case "Change Module":
				return true;
			case "Promote":
				return !cHandler.getNetState().getNetTypeOf(thePeer).getName().equals("BroadcastModule");
			case "Demote":
				return !cHandler.getNetState().getNetTypeOf(thePeer).getName().equals("ListenerModule");
			case "Whisper":
				return !cHandler.getGameState().getLocalPeer().equals(thePeer);
			default:
				return false;
			}
		}
	}

	public static final int ID = 4;
	
	public static final double[] STARTING_RADIAN = new double[] {
		0, // 1 person
		Math.PI / 4, // two people
		Math.PI / 2, // three people
		Math.PI / 4, // four people
	};

	protected static final List<Object> DROPDOWN_CHOICES;
	
	static {
		DROPDOWN_CHOICES = Arrays.asList(new Object[] {
				Arrays.asList(
						"Change Module",
						"Promote",
						"Demote"
				),
				"Whisper"
			});
	}
	
	protected ConnectingHandler cHandler;
	protected List<GraphPoint> broadcastPoints;
	protected List<List<GraphPoint>> tiers;
	
	protected GraphPoint dropdownOn;
	protected DropdownMenu dropdownMenu;
	protected long lastPress;
	
	protected boolean enabled;
	
	@Override
	public void init(GameContainer arg0, StateBasedGame arg1)
			throws SlickException {
		broadcastPoints = new ArrayList<>();
		tiers = new ArrayList<>();
		
		CEventManager.instance.register(this);
		PacketManager.instance.registerClass(this);
		ConnectionPackets.init();
	}

	@Override
	public void render(GameContainer cont, StateBasedGame game, Graphics graphics)
			throws SlickException {
		for(GraphPoint gp : broadcastPoints) {
			gp.render(cont, game, graphics, 200, 120);
		}
		for(List<GraphPoint> gpL : tiers) {
			for(GraphPoint gp : gpL) {
				gp.render(cont, game, graphics, 200, 120);
			}
		}
		
		for(GraphPoint gp : broadcastPoints) {
			gp.renderText(cont, game, graphics, 200, 120);
		}
		for(List<GraphPoint> gpL : tiers) {
			for(GraphPoint gp : gpL) {
				gp.renderText(cont, game, graphics, 200, 120);
			}
		}
		if(dropdownMenu != null)
			dropdownMenu.render(cont, graphics);
	}

	@Override
	public void update(GameContainer cont, StateBasedGame game, int delta)
			throws SlickException {
		if(System.currentTimeMillis() - lastPress > GUtils.CLICK_DELAY) {
			if(Mouse.isButtonDown(1)) {
				CEventManager.instance.broadcast(EventType.MENUS_DESTROYED);
				GraphPoint mouseHoveringOn = getPointMouseHoveringOn(200, 120);
				if(mouseHoveringOn != null) {
					dropdownOn = mouseHoveringOn;
					Point effLoc = (Point) mouseHoveringOn.point.clone();
					effLoc.x += 200;
					effLoc.y += 120;
					dropdownMenu = new DropdownMenu(DROPDOWN_CHOICES, mouseHoveringOn, 
							effLoc);
				}
				lastPress = System.currentTimeMillis();
			}
		}
		
		if(dropdownMenu != null) {
			dropdownMenu.update(cont, delta);
		}
		
		if(System.currentTimeMillis() - lastPress > GUtils.CLICK_DELAY) {
			if(Mouse.isButtonDown(0)) {
				CEventManager.instance.broadcast(EventType.MENUS_DESTROYED);
			}
			lastPress = System.currentTimeMillis();
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			cHandler.destroy();
			System.exit(0);
		}
	}

	protected List<Boolean> decideDropdownChoices(GraphPoint mouseHoveringOn) {
		List<Boolean> result = new ArrayList<>();
		result.add(true);
		result.add(true);
		return result;
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		super.enter(container, game);
		
		recalculateNodeGraph();
		cHandler.begin();
		
		enabled = true;
	}

	
	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		super.leave(container, game);
		
		enabled = false;
	}

	@Override
	public int getID() {
		return ID;
	}

	public void prepare(Peer local, Peer origHostPeer, List<Peer> connectedPeers,
			List<SocketChannel> connections, GameSettings settings) {
		NetState netState = new NetState();
		boolean origHost = origHostPeer == local;
		netState.setLocalPeer(local, 
				origHost ? ModuleHandler.getBroadcastModule() : ModuleHandler.getListenerModule(),
				ModuleHandler.getEmptyModule(), connectedPeers);
		synchronized(connectedPeers) {
			for(int i = 0; i < connectedPeers.size(); i++) {
				Peer peer = connectedPeers.get(i);
				SocketChannel ch = connections.get(i);
				netState.registerPeer(peer, ch, 
						origHostPeer == peer ? ModuleHandler.getBroadcastModule() : ModuleHandler.getListenerModule(),
						ModuleHandler.getEmptyModule(), connectedPeers);
			}
		}
		
		GameState gameState = new GameState(local, connectedPeers, settings);
		
		NetModule netModule = netState.getLocalNetModule();
		LogicModule logModule = netState.getLocalLogicModule();
		
		netModule.setResources(PacketManager.instance, netState, gameState);
		logModule.setResources(PacketManager.instance, netState, gameState);
		
		
		cHandler = new ConnectingHandler(netModule, logModule, netState, gameState);
	}

	/**
	 * Calculate the node graph into broadcastPoints (the inner circle) and
	 * tiers.
	 */
	protected void recalculateNodeGraph() {
		broadcastPoints.clear();
		tiers.clear();
		
		List<Peer> broadcastingPeers = cHandler.getNetState().getPeersWithNetModule("BroadcastModule");
		createCircle(broadcastingPeers, broadcastPoints, new Point(120, 120), 10, true);
		
		List<Peer> other = cHandler.getNetState().getPeersWithNetModule("ListenerModule");
		List<GraphPoint> tmp = new ArrayList<>();
		createCircle(other, tmp, new Point(120, 120), 80, false);
		tiers.add(tmp);
		
		// now go through and add all the peers connections
		// broadcast peers
		for(Peer p : broadcastingPeers) {
			addConnections(p, broadcastPoints, tiers);
		}
		
		// others
		for(Peer p : other) {
			addConnections(p, broadcastPoints, tiers);
		}
	}
	
	protected void addConnections(Peer peer, List<GraphPoint> arr1, List<List<GraphPoint>> arr2) {
		List<Peer> connections = cHandler.getNetState().getConnections(peer);
		
		GraphPoint gp = getGraphPoint(peer, arr1, arr2);
		for(Peer p : connections) {
			gp.addConnectedPoint(getGraphPoint(p, arr1, arr2));
		}
	}

	protected GraphPoint getGraphPoint(Peer peer, List<GraphPoint> arr1, List<List<GraphPoint>> arr2) {
		for(GraphPoint gp : arr1) {
			if(gp.thePeer.equals(peer))
				return gp;
		}
		for(List<GraphPoint> gpL : arr2) {
			for(GraphPoint gp : gpL) {
				if(gp.thePeer.equals(peer))
					return gp;
			}
		}
		return null;
	}

	protected void createCircle(List<Peer> peers,
			List<GraphPoint> points, Point center, int radius, boolean centerWhen1) {
		if(peers.size() == 0)
			return;
		if(centerWhen1 && peers.size() == 1) {
			points.add(new GraphPoint(peers.get(0), new Point(center.x, center.y)));
			return;
		}
		
		double radian = peers.size() <= STARTING_RADIAN.length ? STARTING_RADIAN[peers.size() - 1] : 0;
		double increment = (Math.PI * 2) / peers.size();
		double x, y;
		for(int i = 0; i < peers.size(); i++) {
			x = Math.cos(radian) * radius;
			y = Math.sin(radian) * radius;
			points.add(new GraphPoint(peers.get(i), new Point((int) Math.round(center.x + x), (int) Math.round(center.y + y))));
			radian += increment;
		}
		
	}

	protected GraphPoint getPointMouseHoveringOn(int xShift, int yShift) {
		for(GraphPoint gp : broadcastPoints) {
			if(gp.isBeingHoveredOn(xShift, yShift))
				return gp;
		}
		
		for(List<GraphPoint> lGP : tiers) {
			for(GraphPoint gp : lGP) {
				if(gp.isBeingHoveredOn(xShift, yShift))
					return gp;
			}
		}
		
		return null;
	}
	
	@CEventHandler(event=EventType.MENUS_DESTROYED)
	public void onMenusDestroyed() {
		if(!enabled)
			return;
		dropdownMenu = null;
		dropdownOn = null;
	}
	
	@CEventHandler(event=EventType.DROPDOWN_MENU_CHOICE)
	public void dropdownMenuChoice(DropdownMenu menu, String optionStr, int optionInd) {
		System.out.println("Dropdown Menu Choice: " + optionStr);
		if(!enabled || !menu.equals(dropdownMenu)) {
			return;
		}
		
		GraphPoint gp = dropdownOn;
		Peer peer = gp.thePeer;
		
		switch(optionStr) {
		case "Promote":
			
			if(cHandler.getNetState().getNetTypeOf(peer).getClass().getSimpleName().equals("ListenerModule")) {
				ByteBuffer buffer = NetUtils.createBuffer(cHandler.getGameState().getLocalPeer().getID(), PacketHeader.CHANGE_MODULE);
				try {
					PacketManager.instance.send(PacketHeader.CHANGE_MODULE, buffer, peer, true, "BroadcastModule");
					buffer.flip();
					cHandler.getNetState().getLocalNetModule().sendData(buffer);
					
					
					NetModule netModule = ModuleHandler.getBroadcastModule();
					if(peer.equals(cHandler.getNetState().getLocalPeer())) {
						netModule.setResources(PacketManager.instance, cHandler.getNetState(), cHandler.getGameState());
					}
					cHandler.getNetState().setNetModuleOf(peer, netModule);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			recalculateNodeGraph();
			break;
			
		case "Demote":
			if(cHandler.getNetState().getNetTypeOf(peer).getName().equals("BroadcastModule")) {
				ByteBuffer buffer = NetUtils.createBuffer(cHandler.getGameState().getLocalPeer().getID(), PacketHeader.CHANGE_MODULE);
				try {
					PacketManager.instance.send(PacketHeader.CHANGE_MODULE, buffer, peer, true, "ListenerModule");
					buffer.flip();
					cHandler.getNetState().getLocalNetModule().sendData(buffer);
					
					NetModule netModule = ModuleHandler.getListenerModule();
					if(peer.equals(cHandler.getNetState().getLocalPeer())) {
						netModule.setResources(PacketManager.instance, cHandler.getNetState(), cHandler.getGameState());
					}
					cHandler.getNetState().setNetModuleOf(peer, netModule);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			recalculateNodeGraph();
			break;
		case "Whisper":
			if(peer.equals(cHandler.getNetState().getLocalPeer()))
				break;
			
			
			String msg = JOptionPane.showInputDialog("What do you want to send to " + peer.getName() + "?");
			
			ByteBuffer buffer = NetUtils.createBufferNoID(PacketHeader.WHISPER);
			PacketManager.instance.send(PacketHeader.WHISPER, buffer, peer, msg); // This is located in ConnectionPackets or whatever
			buffer.flip();
			try {
				cHandler.getNetState().getLocalNetModule().ensureDirectConnection(peer);
				cHandler.getNetState().getLocalNetModule().sendDirectly(peer, buffer);
				cHandler.getNetState().getLocalNetModule().destroyUnnecessaryConnection(peer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}
	
	@PacketHandler(header=PacketHeader.CHANGE_MODULE, priority=10) // don't do anything but monitor
	public void onChangeModule(Peer peer, ParsedPacket parsedPacket) {
		if(!enabled) {
			System.err.println("Recieved onChangeModule but was not enabled");
			return;
		}
		recalculateNodeGraph();
	}

	@PacketHandler(header=PacketHeader.DIRECT_PACKET, priority=10) 
	public void onWhispered(final Peer peer, ParsedPacket wrappedPacket) { // monitor
		DirectConnectionPacket dcp = (DirectConnectionPacket) wrappedPacket;
		ParsedPacket realPacket = dcp.getPacket();
		if(realPacket.getHeader() != PacketHeader.WHISPER)
			return;
		
		final WhisperPacket whispPacket = (WhisperPacket) realPacket;
		System.out.println(peer.getName() + " says... '" + whispPacket.getMessage() + "'");
		new Thread(new Runnable() {

			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, whispPacket.getMessage(), peer.getName() + " says...", JOptionPane.INFORMATION_MESSAGE);
			}
			
		}).start();
	}
	
}
