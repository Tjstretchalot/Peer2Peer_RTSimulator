package me.timothy.dcrts.net.lobby;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.timothy.dcrts.DCRTSEntry;
import me.timothy.dcrts.net.packets.AssignIDPacket;
import me.timothy.dcrts.net.packets.ChangeNamePacket;
import me.timothy.dcrts.net.packets.ConnectPacket;
import me.timothy.dcrts.net.packets.CountdownChangedPacket;
import me.timothy.dcrts.net.packets.DisconnectPacket;
import me.timothy.dcrts.net.packets.PingPacket;
import me.timothy.dcrts.net.packets.SetReadyPacket;
import me.timothy.dcrts.net.packets.UpdateSettingsPacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.LocalPeer;
import me.timothy.dcrts.peer.OtherPeer;
import me.timothy.dcrts.peer.PartialPeer;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.settings.GameSettings;
import me.timothy.dcrts.state.ConnectionState;
import me.timothy.dcrts.utils.NetUtils;

public class HostedLobby extends Lobby implements PacketListener {
	public static final int COUNTDOWN_SECONDS = 5;
	
	private ServerSocketChannel servChannel;
	private List<SocketChannel> connections;
	private GameSettings settings;
	
	private Thread connThread;
	private ConnectionListener connListener;
	private Thread connChanThread;
	private ConnectionChannelReader connChanReader;
	
	private int idCounter;
	
	private Thread countingThread;
	private int countdown;
	private boolean counting;
	
	protected class ConnectionListener implements Runnable {
		@Override
		public void run() {
			while(servChannel.isOpen()) {
				try {
					SocketChannel incoming = servChannel.accept();
					incoming.configureBlocking(false);
					synchronized(connections) {
						connectedPeers.add(new PartialPeer());
						connections.add(incoming);
					}
				}catch(AsynchronousCloseException e) {
					break;
				}catch (IOException e) {
					e.printStackTrace();
					break;
				}
				
			}
		}
	}
	
	protected class ConnectionChannelReader implements Runnable {

		@Override
		public void run() {
			ByteBuffer buffer = ByteBuffer.allocate(512);
			while(servChannel.isOpen()) {
				try {
					boolean readSomething = false;
					synchronized(connections) {
						for(SocketChannel sch : connections) {
							int numBytes = sch.read(buffer);
							
							if(numBytes == 0) {
								continue;
							}
							readSomething = true;
							if(numBytes == buffer.capacity()) {
								System.err.println("Too many bytes were sent! May have overflowed!");
							}
							buffer.flip();
							
							while(buffer.remaining() > 0) {
								parseNextPacket(buffer);
							}
							
							buffer.clear();
						}
					}
					if(!readSomething)
						Thread.sleep(1);
					else
						Thread.yield();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (InterruptedException e) {
					break;
				} 
			}
		}

		private void parseNextPacket(ByteBuffer buffer) {
			int peerId = buffer.getInt();
			int headerInt = buffer.getInt();
			
			Peer peer = NetUtils.getPeerByID(connectedPeers, peerId);
			PacketHeader header = PacketHeader.byValue(headerInt);
			
			ParsedPacket parsed = PacketManager.instance.parse(header, buffer);
			PacketManager.instance.broadcastPacket(peer, parsed);
		}
		
	}
	
	public HostedLobby() {
		idCounter = NetUtils.INIT_ID;
	}
	
	@Override
	public void changeName(String newName) {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.CHANGE_NAME);
		PacketManager.instance.send(PacketHeader.CHANGE_NAME, buffer, newName);
		buffer.flip();
		
		sendToAll(buffer);
		
		localPeer.setName(newName);
	}

	@Override
	public GameSettings getSettings() {
		return settings;
	}

	@Override
	public void updateSettings(GameSettings settings) {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.UPDATE_SETTINGS);
		PacketManager.instance.send(PacketHeader.UPDATE_SETTINGS, buffer, settings);
		
		buffer.flip();
		sendToAll(buffer);
		this.settings = settings;
	}

	@Override
	public void setReady(boolean ready) {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.SET_READY);
		PacketManager.instance.send(PacketHeader.SET_READY, buffer, ready);
		
		buffer.flip();
		sendToAll(buffer);
		
		localPeer.setReady(ready);
	}
	
	@Override
	public void beginCountdown() {
		if(counting)
			return;
		counting = true;
		countdown = COUNTDOWN_SECONDS;
		PacketManager.instance.broadcastPacket(localPeer, new CountdownChangedPacket(countdown));
		
		countingThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while(counting && countdown > 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
					countdown--;
					PacketManager.instance.broadcastPacket(localPeer, new CountdownChangedPacket(countdown));
				}
			}
			
		});
		countingThread.start();
		
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.BEGIN_COUNTDOWN);
		PacketManager.instance.send(PacketHeader.BEGIN_COUNTDOWN, buffer, COUNTDOWN_SECONDS, System.currentTimeMillis());
		buffer.flip();
		sendToAll(buffer);
	}

	@Override
	public void begin() {
		super.begin();
		System.out.println("--Beginning Server--");
		try {
			connections = new ArrayList<>();
			connections = Collections.synchronizedList(connections);
			
			servChannel = ServerSocketChannel.open();
			servChannel.bind(new InetSocketAddress(NetUtils.PORT));
			
			connListener = new ConnectionListener();
			connChanReader = new ConnectionChannelReader();
			
			localPeer = new LocalPeer("Host", 0);
			settings = new GameSettings();

			PacketManager.instance.registerClass(this);
			connThread = new Thread(connListener);
			connChanThread = new Thread(connChanReader);
			connThread.start();
			connChanThread.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void interruptReady() {
		if(!counting)
			return;
		
		localPeer.setReady(false);
		countdown = COUNTDOWN_SECONDS;
		counting = false;
		countingThread.interrupt();
		countingThread = null;
		
		
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.INTERRUPT_READY);
		PacketManager.instance.send(PacketHeader.INTERRUPT_READY, buffer);
		buffer.flip();
		sendToAll(buffer);
	}
	
	@Override
	public void destroy() {
		System.out.println("--Destroying Host--");
		try {
			servChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		connThread.interrupt();
		connChanThread.interrupt();
		Thread.yield();
		
		PacketManager.instance.unregisterClass(this);
		connections = null;
		connThread = null;
		connChanThread = null;
		settings = null;
		connListener = null;
		connChanReader = null;
		servChannel = null;
		idCounter = 0;
		super.destroy();
	}
	
	public void shutdownServer() {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.DISCONNECT);
		PacketManager.instance.send(PacketHeader.DISCONNECT, buffer, "Server shutting down");
		buffer.flip();
		synchronized(connectedPeers) {
			while(connections.size() > 0) {
				SocketChannel sc = connections.get(0);
				
				try {
					sc.write(buffer);
					sc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				buffer.rewind();
				connections.remove(0);
				connectedPeers.remove(0);
			}
		}
	}
	
	@Override
	public void ping() {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.PING);
		PacketManager.instance.send(PacketHeader.PING, buffer, System.currentTimeMillis());
		buffer.flip();
		
		sendToAll(buffer);
	}
	
	protected void writeToPeer(Peer peer, ByteBuffer buffer) {
		synchronized(connectedPeers) { // I love thread safety
			int ind = connectedPeers.indexOf(peer);
			try {
				connections.get(ind).write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void sendToAll(ByteBuffer buffer, Peer... exceptAr) {
		List<Peer> except = Arrays.asList(exceptAr);
		if(except.size() == connectedPeers.size()) {
			return;
		}
		synchronized(connectedPeers) {
			int counter = 0;
			for(SocketChannel connection : connections) {
				Peer peer = connectedPeers.get(counter);
				counter++;
				if(except.contains(peer))
					continue;
				try {
					connection.write(buffer);
				} catch (IOException e) {
					e.printStackTrace();
				}
				buffer.rewind();
			}
		}
	}

	private void suggestId(Peer peer) {
		PacketHeader header = PacketHeader.ASSIGN_ID; 
		idCounter++;
		final int assID = idCounter;
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), header);
		PacketManager.instance.send(header, buffer, false, assID, peer.getID());
		buffer.flip();
		writeToPeer(peer, buffer);
	}

	// listeners
	
	// respond to pings
	@PacketHandler(priority=3, header=PacketHeader.PING) 
	public void onPinged(Peer peer, ParsedPacket parsedPacket) {
		PingPacket packet = (PingPacket) parsedPacket;
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.RETURN_PING);
		PacketManager.instance.send(PacketHeader.RETURN_PING, buffer, packet.getTimeSent());
		buffer.flip();
		writeToPeer(peer, buffer);
	}
	
	// handle connections
	@PacketHandler(priority=3, header=PacketHeader.CONNECT)
	public void onConnect(Peer peer, ParsedPacket packet) {
		PartialPeer partPeer = (PartialPeer) peer;
		
		ConnectPacket conPacket = (ConnectPacket) packet;
		partPeer.setName(conPacket.getName());
		suggestId(partPeer);
		
		synchronized(connectedPeers) {
			ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.CONNECT);
			PacketManager.instance.send(PacketHeader.CONNECT, buffer, localPeer);
			buffer.flip();
			
			writeToPeer(peer, buffer);
			for(Peer p : connectedPeers) {
				if(p.equals(partPeer))
					continue;
				buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.CONNECT);
				PacketManager.instance.send(PacketHeader.CONNECT, buffer, p);
				buffer.flip();
				writeToPeer(peer, buffer);
			}
		}
	}
	
	@PacketHandler(priority=3, header=PacketHeader.ASSIGN_ID)
	public void onAssignID(Peer peer, ParsedPacket packet) {
		AssignIDPacket assignIDPacket = (AssignIDPacket) packet;
		
		boolean requesting = assignIDPacket.requestingID();
		if(requesting) {
			suggestId(peer);
			return;
		}else {
			PartialPeer partPeer = (PartialPeer) peer;
			final int ind = connectedPeers.indexOf(partPeer);
			int wantedID = assignIDPacket.getID();
			// check for a clash
			synchronized(connectedPeers) {
				for(Peer p : connectedPeers) {
					if(p == partPeer)
						continue;
					if(p.getID() == wantedID) {
						System.err.println("Peer asked for a used id!");
						suggestId(peer);
						return;
					}
				}
				// upgrade that fool
				partPeer.setID(wantedID);
				System.out.println(partPeer.getName() + " now has the id " + wantedID);
			}
			OtherPeer otherPeer = new OtherPeer(partPeer);
			connectedPeers.set(ind, otherPeer);
			ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.CONNECT);
			PacketManager.instance.send(PacketHeader.CONNECT, buffer, true, false, wantedID, otherPeer.getName());
			buffer.flip();
			sendToAll(buffer, otherPeer);
		}
	}
	
	@PacketHandler(priority=3, header=PacketHeader.DISCONNECT)
	public void onDisconnect(Peer peer, ParsedPacket packet) {
		synchronized(connectedPeers) {
			int ind = connectedPeers.indexOf(peer);
			connections.remove(ind);
			connectedPeers.remove(ind);
		}
		DisconnectPacket discPacket = (DisconnectPacket) packet;
		handleChat(peer, peer.getName() + " has disconnected (" + discPacket.getReason() + ").");
		if(peer instanceof OtherPeer) {
			ByteBuffer buffer = NetUtils.createBuffer(peer.getID(), PacketHeader.DISCONNECT);
			PacketManager.instance.send(PacketHeader.DISCONNECT, buffer, discPacket.getReason());
			buffer.flip();
			sendToAll(buffer);
		}
	}
	
	@PacketHandler(priority=3, header=PacketHeader.UPDATE_SETTINGS) 
	public void onUpdateSettings(Peer peer, ParsedPacket packet) {
		UpdateSettingsPacket uSettingsPacket = (UpdateSettingsPacket) packet;
		
		ByteBuffer buffer = NetUtils.createBuffer(peer.getID(), PacketHeader.UPDATE_SETTINGS);
		PacketManager.instance.send(PacketHeader.UPDATE_SETTINGS, buffer, uSettingsPacket);
		buffer.flip();
		sendToAll(buffer);
	}
	
	@PacketHandler(priority=3, header=PacketHeader.BEGIN_COUNTDOWN)
	public void onBeginCountdown(Peer peer, ParsedPacket packet) {
		if(counting)
			return;
		beginCountdown();
	}
	
	@PacketHandler(priority=3, header=PacketHeader.SYNC_COUNTDOWN)
	public void onCountdownChange(Peer peer, ParsedPacket packet) {
		if(peer != localPeer) {
			// ...what?
			ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.ERROR);
			PacketManager.instance.send(PacketHeader.ERROR, buffer, "Countdown is handled by host");
			buffer.flip();
			
			writeToPeer(peer, buffer);
			return;
		}
		
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.SYNC_COUNTDOWN);
		PacketManager.instance.send(PacketHeader.SYNC_COUNTDOWN, buffer, countdown);
		buffer.flip();
		
		sendToAll(buffer);
		
		if(countdown <= 0) {
			// *tear*, they grow up so fast!
			buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.SEND_NET_INFO);
			Object[] args = null;
			synchronized(connectedPeers) {
				args = new Object[connections.size() * 2 + 1];
				args[0] = connections.size();
				for(int i = 0; i < connections.size(); i++) {
					args[i * 2 + 1] = connectedPeers.get(i).getID();
					try {
						args[i * 2 + 2] = connections.get(i).getRemoteAddress();
					} catch (IOException e) {
						// this will never happen
						e.printStackTrace();
					}
				}
			}
			PacketManager.instance.send(PacketHeader.SEND_NET_INFO, buffer, args);
			buffer.flip();
			
			sendToAll(buffer);
			
			((ConnectionState) DCRTSEntry.GAME_STATES[ConnectionState.ID]).prepare(localPeer, localPeer, connectedPeers, connections, settings);
			DCRTSEntry.instance.enterState(ConnectionState.ID);
		}
	}
	
	@PacketHandler(priority=3, header=PacketHeader.CHANGE_NAME)
	public void onChangeName(Peer peer, ParsedPacket packet) {
		ChangeNamePacket chNamePacket = (ChangeNamePacket) packet;
		peer.setName(chNamePacket.getNewName());
		
		ByteBuffer buffer = NetUtils.createBuffer(peer.getID(), PacketHeader.CHANGE_NAME);
		PacketManager.instance.send(PacketHeader.CHANGE_NAME, buffer, chNamePacket.getNewName());
		buffer.flip();
		sendToAll(buffer, peer);
	}
	
	@PacketHandler(priority=3, header=PacketHeader.SET_READY)
	public void onSetReady(Peer peer, ParsedPacket packet) {
		SetReadyPacket sRdPacket = (SetReadyPacket) packet;
		ByteBuffer buffer = NetUtils.createBuffer(peer.getID(), PacketHeader.SET_READY);
		PacketManager.instance.send(PacketHeader.SET_READY, buffer, sRdPacket.ready());
		
		buffer.flip();
		sendToAll(buffer, peer);
		
		peer.setReady(sRdPacket.ready());
	}
	
	@PacketHandler(priority=3, header=PacketHeader.INTERRUPT_READY) 
	public void onInterruptReady(Peer peer, ParsedPacket packet) {
		countdown = COUNTDOWN_SECONDS;
		counting = false;
		countingThread.interrupt();
		countingThread = null;
		
		peer.setReady(false);
		
		ByteBuffer buffer = NetUtils.createBuffer(peer.getID(), PacketHeader.INTERRUPT_READY);
		PacketManager.instance.send(PacketHeader.INTERRUPT_READY, buffer);
		buffer.flip();
		sendToAll(buffer, peer);
	}
}
