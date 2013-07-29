package me.timothy.dcrts.net.lobby;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import me.timothy.dcrts.DCRTSEntry;
import me.timothy.dcrts.net.packets.AssignIDPacket;
import me.timothy.dcrts.net.packets.ChangeNamePacket;
import me.timothy.dcrts.net.packets.ConnectPacket;
import me.timothy.dcrts.net.packets.DisconnectPacket;
import me.timothy.dcrts.net.packets.PingPacket;
import me.timothy.dcrts.net.packets.SendNetInfoPacket;
import me.timothy.dcrts.net.packets.SetReadyPacket;
import me.timothy.dcrts.net.packets.UpdateSettingsPacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.LocalPeer;
import me.timothy.dcrts.peer.OtherPeer;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.settings.GameSettings;
import me.timothy.dcrts.state.ConnectionState;
import me.timothy.dcrts.state.MainMenuState;
import me.timothy.dcrts.state.MessageState;
import me.timothy.dcrts.utils.ErrorUtils;
import me.timothy.dcrts.utils.NetUtils;

import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

public class ConnectedLobby extends Lobby implements PacketListener {
	private SocketChannel connection;
	private SocketAddress address;

	private IncomingPacketReader incReader;
	private Thread incReaderThread;

	private GameSettings gameSettings;

	private Peer serverPeer; // this peer is included with the connectedPeers
	private boolean destroying;

	protected class IncomingPacketReader implements Runnable {
		@Override
		public void run() {
			ByteBuffer buffer = ByteBuffer.allocate(512);
			while(connection != null && connection.isOpen()) {
				try {
					int numBytes = -1;
					synchronized(connection) {
						numBytes = connection.read(buffer);
					}
					if(numBytes == 0) {
						Thread.sleep(1);
						continue;
					}
					if(numBytes == buffer.capacity()) {
						throw new IOException("May have overflowed!");
					}

					buffer.flip();

					while(buffer.remaining() > 0) {
						if(connectedPeers == null)
							break; // we've been destroyed! :(
						parseNextPacket(buffer);
					}

					buffer.clear();
					Thread.yield();
				} catch (IOException | InterruptedException e) {
					if(!destroying) {
						System.err.println("Connection lost (CLIENT to SERVER)");
						destroy();
						DCRTSEntry.instance.enterState(MainMenuState.ID, new EmptyTransition(), new FadeInTransition());
					}
					break;
				}
			}
		}

		private void parseNextPacket(ByteBuffer buffer) {
			int peerId = buffer.getInt();
			int headerInt = buffer.getInt();

			Peer peer = NetUtils.getPeerByID(connectedPeers, peerId);
			PacketHeader header = PacketHeader.byValue(headerInt);
			if(header == null) {
				System.out.println("[ConnectedLobby] No header with the id " + headerInt + " detected!");
				return;
			}
			
			ParsedPacket parsed = PacketManager.instance.parse(header, buffer);
			PacketManager.instance.broadcastPacket(peer, parsed);
		}
	}

	/**
	 * Sends the specified buffer to the host. Does not flip
	 * or rewind the buffer
	 * 
	 * @param buffer the buffer
	 */
	protected void send(ByteBuffer buffer) {
		synchronized(connection) {
			try {
				connection.write(buffer); // throwing INTERRUPT exception when always rendering
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void changeName(String newName) {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.CHANGE_NAME);
		PacketManager.instance.send(PacketHeader.CHANGE_NAME, buffer, newName);
		buffer.flip();

		send(buffer);

		localPeer.setName(newName);
	}

	@Override
	public GameSettings getSettings() {
		return gameSettings;
	}

	@Override
	public void updateSettings(GameSettings settings) {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.UPDATE_SETTINGS);
		PacketManager.instance.send(PacketHeader.UPDATE_SETTINGS, buffer, settings);

		buffer.flip();
		send(buffer);
		gameSettings = settings;
	}

	@Override
	public void setReady(boolean ready) {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.SET_READY);
		PacketManager.instance.send(PacketHeader.SET_READY, buffer, ready);

		buffer.flip();
		send(buffer);

		localPeer.setReady(ready);
	}

	@Override
	public void beginCountdown() {
		//		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.BEGIN_COUNTDOWN);
		//		PacketManager.instance.send(PacketHeader.BEGIN_COUNTDOWN, buffer);
		//		
		//		buffer.flip();
		//		send(buffer);
	}

	@Override
	public void interruptReady() {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.INTERRUPT_READY);
		PacketManager.instance.send(PacketHeader.INTERRUPT_READY, buffer);

		buffer.flip();
		send(buffer);

		localPeer.setReady(false);
	}

	@Override
	public void begin() {
		super.begin();
		System.out.println("--Beginning Client--");
		if(address == null)
			ErrorUtils.nullPointer(new String[] { "address" }, address);

		destroying = false;
		localPeer = new LocalPeer("Client", NetUtils.RESERVED_ID);
		try {
			connection = SocketChannel.open(address);
			connection.configureBlocking(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		PacketManager.instance.registerClass(this);

		incReader = new IncomingPacketReader();
		incReaderThread = new Thread(incReader);
		incReaderThread.start();

		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.CONNECT);
		PacketManager.instance.send(PacketHeader.CONNECT, buffer, false, false, "Client");
		buffer.flip();
		send(buffer);
	}

	@Override
	public void destroy() {
		if(destroying)
			return;
		System.out.println("--Destroying Client--");
		destroying = true;
		address = null;
		incReaderThread.interrupt();
		incReader = null;
		PacketManager.instance.unregisterClass(this);

		if(connection != null && connection.isOpen()) {
			ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.DISCONNECT);
			PacketManager.instance.send(PacketHeader.DISCONNECT, buffer, "Client destroyed");
			buffer.flip();

			send(buffer);
			try {
				connection.close();
			} catch (IOException e) {
				System.err.println("Error occurred closing connection");
			}
		}
		connection = null;
		super.destroy();
	}

	@Override
	public void ping() {
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.PING);
		PacketManager.instance.send(PacketHeader.PING, buffer, System.currentTimeMillis());
		buffer.flip();

		send(buffer);
	}

	public void setAddress(SocketAddress addr) {
		address = addr;
	}

	@PacketHandler(priority=3, header=PacketHeader.PING) 
	public void onPinged(Peer peer, ParsedPacket parsedPacket) {
		PingPacket packet = (PingPacket) parsedPacket;
		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.RETURN_PING);
		PacketManager.instance.send(PacketHeader.RETURN_PING, buffer, packet.getTimeSent());
		buffer.flip();
		send(buffer);
	}

	@PacketHandler(priority=3, header=PacketHeader.ASSIGN_ID)
	public void onAssignID(Peer peer, ParsedPacket parsedPacket) {
		AssignIDPacket packet = (AssignIDPacket) parsedPacket;

		if(packet.requestingID()) {
			ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.ERROR);
			PacketManager.instance.send(PacketHeader.ERROR, buffer, "You should talk to the host about that!");
			buffer.flip();

			send(buffer);
			return;
		}

		if(packet.getPeerID() != localPeer.getID()) {
			Peer thePeer = getPeerByID(packet.getPeerID());
			if(thePeer == null) {
				ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.ERROR);
				PacketManager.instance.send(PacketHeader.ERROR, buffer, 
						"I don't know any peer with id " + packet.getPeerID() + "!");
				buffer.flip();

				send(buffer);
				return;
			}
			thePeer.setID(packet.getID());
			return;
		}

		// he's suggesting an id! How considerate
		int suggestedID = packet.getID();

		ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.ASSIGN_ID);
		PacketManager.instance.send(PacketHeader.ASSIGN_ID, buffer, false, suggestedID, localPeer.getID());
		buffer.flip();
		send(buffer);

		localPeer.setID(suggestedID);
		System.out.println("Set my id to " + suggestedID);
	}

	@PacketHandler(priority=3, header=PacketHeader.UPDATE_SETTINGS)
	public void updateSettings(Peer peer, ParsedPacket packet) {
		UpdateSettingsPacket uSettingsPacket = (UpdateSettingsPacket) packet;

		gameSettings = uSettingsPacket.getSettings();
	}

	@PacketHandler(priority=3, header=PacketHeader.CONNECT)
	public void onConnect(Peer peer, ParsedPacket parsedPacket) {
		ConnectPacket packet = (ConnectPacket) parsedPacket;
		if(NetUtils.getPeerByID(connectedPeers, packet.getID()) != null) {
			ByteBuffer buffer = NetUtils.createBuffer(localPeer.getID(), PacketHeader.ERROR);
			PacketManager.instance.send(PacketHeader.ERROR, buffer, "A peer is already connected with that id!");
			buffer.flip();

			send(buffer);
			return;
		}

		Peer newPeer = new OtherPeer(packet.getName(), packet.getID());
		newPeer.setReady(packet.ready());

		synchronized(connectedPeers) {
			if(connectedPeers.size() == 0) {
				serverPeer = newPeer;
			}
			connectedPeers.add(newPeer);
		}

		System.out.println("Peer connected: " + newPeer.getName() + "(" + newPeer.getID() + ")");
	}

	@PacketHandler(priority=3, header=PacketHeader.DISCONNECT)
	public void onDisconnect(Peer peer, ParsedPacket packet) {
		DisconnectPacket discPacket = (DisconnectPacket) packet;
		String reason = discPacket.getReason();

		MessageState mesState = (MessageState) DCRTSEntry.GAME_STATES[MessageState.ID];
		if(peer == null) {
			destroy();
			mesState.prepare("Unknown Peer Disconnected: " + reason, 5, MainMenuState.ID);
			DCRTSEntry.instance.enterState(MessageState.ID);
			return;
		}else if(peer.equals(serverPeer)) { 
			connection = null; // don't attempt anything
			destroy();
			mesState.prepare("Host Disconnected: " + reason, 5, MainMenuState.ID);
			DCRTSEntry.instance.enterState(MessageState.ID);
			return;
		}else if(peer.equals(localPeer)) {
			destroy();
			mesState.prepare("You have been disconnected: " + reason, 5, MainMenuState.ID);
			DCRTSEntry.instance.enterState(MessageState.ID);
			return;
		}else {
			System.out.println("Peer " + peer.getName() + " disconnected");
			connectedPeers.remove(peer);
			// TODO send message to chat
		}
	}

	@PacketHandler(priority=3, header=PacketHeader.CHANGE_NAME)
	public void onChangeName(Peer peer, ParsedPacket packet) {
		ChangeNamePacket chNamePacket = (ChangeNamePacket) packet;
		peer.setName(chNamePacket.getNewName());
	}

	@PacketHandler(priority=3, header=PacketHeader.SET_READY)
	public void onSetReady(Peer peer, ParsedPacket packet) {
		SetReadyPacket sRdPacket = (SetReadyPacket) packet;

		peer.setReady(sRdPacket.ready());
	}

	@PacketHandler(priority=3, header=PacketHeader.INTERRUPT_READY) 
	public void onInterruptReady(Peer peer, ParsedPacket packet) {
		peer.setReady(false);
	}

	@PacketHandler(priority=3, header=PacketHeader.SEND_NET_INFO)
	public void onStartGame(Peer peer, ParsedPacket packet) {
		final SendNetInfoPacket snip = (SendNetInfoPacket) packet;
		final MessageState messageState = (MessageState) DCRTSEntry.GAME_STATES[MessageState.ID];
		messageState.prepare("Creating connections", 9999, -1);
		DCRTSEntry.instance.enterState(MessageState.ID, new FadeOutTransition(), new FadeInTransition());

		/*
		 * This opens up a connection on NetUtils#PORT + (rid - INIT_ID), so 
		 * know port is used multiple times (which results in Already in use 
		 * exceptions). So the host has an id of 1337 (same as INIT_ID), and thus
		 * gets port NetUtils#PORT. The second person gets port NetUtils#PORT + 1, 
		 * then NetUtils#PORT + 2, etc.
		 * 
		 * Further implementation of this id is found in LobbyPackets, which creates the remote
		 * addresses following this port theme when parsing SEND_NET_INFO packets
		 */
		new Thread(new Runnable() {

			@Override
			public void run() {
				final int con = connectedPeers.size() - 1;
				final SocketChannel[] connections = new SocketChannel[connectedPeers.size()];
				int numBelowMyIDTmp = 0;

				for(int i = 0; i < connectedPeers.size(); i++) {
					if(connectedPeers.get(i) == serverPeer) {
						connections[i] = connection;
						continue;
					}
					if(connectedPeers.get(i).getID() < localPeer.getID()) 
						numBelowMyIDTmp++;
				}
				final int numBelowMyID = numBelowMyIDTmp;
				System.out.println("numBelowMyID = " + numBelowMyID);
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							final int port = NetUtils.PORT + (localPeer.getID() - NetUtils.INIT_ID);
							System.out.println("Opening server socket channel");
							ServerSocketChannel ch = ServerSocketChannel.open();
							ch.bind(new InetSocketAddress(port));
							System.out.println("Done, accepting connections on " + port);
							ByteBuffer buffer = ByteBuffer.allocate(4);
							for(int i = 0; i < numBelowMyID; i++) {
								System.out.println("Inside loop");
								SocketChannel sc = ch.accept();
								System.out.println("Accepted connection from " + sc.getRemoteAddress());
								sc.configureBlocking(true);
								System.out.println("Reading into buffer");
								sc.read(buffer);
								System.out.println("Done, parsing");
								sc.configureBlocking(false);
								buffer.flip();

								int id = buffer.getInt();
								buffer.clear();

								for(int j = 0; j < con + 1; j++) {
									System.out.println("Here1");
									if(connectedPeers.get(j).getID() == id) {
										System.out.println("Here2");
										InetSocketAddress expected = snip.getAddress(id); // TODO compare expected HOST NAME to end HOST NAME
										if(!expected.getHostName().equals(((InetSocketAddress) sc.getRemoteAddress()).getHostName())) {
											System.err.println("Expected address and remote address don't match (" + id + ", " + 
													expected.getHostName() + " vs " + 
													((InetSocketAddress) sc.getRemoteAddress()).getHostName() + ")");
										}
										connections[j] = sc;
										break;
									}
									System.out.println("Here3");
								}
								System.out.println("Done, " + id + " connected");
							}
							ch.close();
							System.out.println("Recieved all incoming connections (" + numBelowMyID + "/" + 
									con + ")");
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

				}).start();

				ByteBuffer buffer = ByteBuffer.allocate(4);
				buffer.putInt(localPeer.getID());
				buffer.flip();
				for(int i = 0; i < connectedPeers.size(); i++) {
					int rid = connectedPeers.get(i).getID();
					if(rid < localPeer.getID())
						continue;
					SocketChannel sc = null;
					try {
						System.out.println("Connecting to " + snip.getAddress(rid));
						sc = SocketChannel.open(snip.getAddress(rid));
						sc.configureBlocking(true);
						sc.write(buffer);
						sc.configureBlocking(false);
					} catch (IOException e) {
						e.printStackTrace();
					}

					buffer.rewind();

					connections[i] = sc;
				}

				System.out.println("Created all outgoing connections (" + 
						(connectedPeers.size() - 1 - numBelowMyID) + "/" + con + ")");

				while(true) {
					boolean cont = false;
					for(int i = 0; i < connections.length; i++) {
						if(connections[i] == null) {
							cont = true;
							break;
						}
					}
					if(!cont)
						break;

					try {
						Thread.sleep(10);
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}

				System.out.println("All connections completed");

				ConnectionState connState = (ConnectionState) DCRTSEntry.GAME_STATES[ConnectionState.ID];
				connState.prepare(localPeer, serverPeer, connectedPeers, Arrays.asList(connections), gameSettings);
				DCRTSEntry.instance.enterState(ConnectionState.ID, new FadeOutTransition(), new FadeInTransition());
			}


		}).start();

	}
}
