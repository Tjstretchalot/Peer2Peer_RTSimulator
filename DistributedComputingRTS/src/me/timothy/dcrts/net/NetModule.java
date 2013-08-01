package me.timothy.dcrts.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.timothy.dcrts.net.packets.DirectConnectionPacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.NetUtils;

/**
 * Handles incoming packets by broadcasting them to the PacketManager.
 * 
 * Sends packets based on ClientEvents
 * @author Timothy
 */
public abstract class NetModule extends Module {
	protected ServerSocketChannel directConnectionServerSocket;
	
	private DirectConnectionAcceptionThread dcat;
	private DirectConnectionMonitorThread dcmt;
	private class DirectConnectionAcceptionThread extends Thread {
		@Override
		public void run() {
			while(true) {
				try {
					SocketChannel channel = directConnectionServerSocket.accept();
					onConnectionAcception(channel);
				} catch (IOException e) {
					if(e instanceof ClosedByInterruptException)
						break;
					e.printStackTrace();
				}
				
			}
		}
	}
	
	private class DirectConnectionMonitorThread extends Thread implements PacketListener {
		List<Peer> monitoring;
		
		DirectConnectionMonitorThread() {
			monitoring = Collections.synchronizedList(new ArrayList<Peer>());
		}
		
		@Override
		public void run() {
			List<Peer> toRem = new ArrayList<>();
			ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.getLargestPacketSize());
			while(true) {
				synchronized(monitoring) { 
					for(Peer peer : monitoring) {
						if(!peer.metaData.containsKey("directConnection")) {
							toRem.add(peer);
							continue;
						}
						
						SocketChannel sc = (SocketChannel) peer.metaData.get("directConnection");
						int read;
						try {
							read = sc.read(buffer);
						} catch (IOException e) {
							e.printStackTrace();
							toRem.add(peer);
							continue;
						}
						if(read == 0)
							break;
						
						buffer.flip();
						
						int headerInt = buffer.getInt();
						System.out.println("headerInt: " + headerInt);
						PacketHeader header = PacketHeader.byValue(headerInt);
						ParsedPacket parsed = PacketManager.instance.parse(header, buffer);
						
						ParsedPacket wrapped = new DirectConnectionPacket(parsed);
						PacketManager.instance.broadcastPacket(peer, wrapped);
						
						buffer.clear();
					}
					monitoring.removeAll(toRem);
				}
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		
		@SuppressWarnings("unused")
		@PacketHandler(header=PacketHeader.DIRECT_PACKET, priority=1)
		public void onDestroyConnection(Peer peer, ParsedPacket packet) {
			DirectConnectionPacket dcp = (DirectConnectionPacket) packet;
			if(dcp.getPacket().getHeader() != PacketHeader.DESTROYING_CHANNEL)
				return;
			
			stopMonitoring(peer);
		}
	}
	
	protected NetModule() {
		
	}
	
	@Override
	public void onActivate() {
		try {
			int id = gameState.getLocalPeer().getID();
			int port = NetUtils.getDirectPort(id);
			System.out.println("NetModule Direct-Connection Port: " + port + " (ID=" + id + ")");
			directConnectionServerSocket = ServerSocketChannel.open();
			directConnectionServerSocket.bind(new InetSocketAddress(port));
			directConnectionServerSocket.configureBlocking(true);
			
			dcat = new DirectConnectionAcceptionThread();
			dcat.start();
			
			dcmt = new DirectConnectionMonitorThread();
			dcmt.start();
			
			pManager.registerClass(dcmt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDeactivate() {
		dcat.interrupt();
		dcmt.interrupt();
		try {
			directConnectionServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		pManager.unregisterClass(dcmt);
	}
	
	/**
	 * Sends the buffer to everyone
	 * @param buffer the buffer
	 * @param except may or may not do anything, may be empty
	 * @throws IOException if an i/o exception occurs
	 */
	public abstract void sendData(ByteBuffer buffer, Peer... except) throws IOException;

	/**
	 * Ensures there is a direct connection to the specified peer, regardless
	 * if that peer is connected logically. This may be used to send secure
	 * messages.
	 * 
	 * 
	 * Encryption is up to the implementation
	 * @param peer the peer 
	 */
	public void ensureDirectConnection(Peer peer) throws IOException {
		if(getMonitoringChannel(peer) != null)
			return;
		
		InetSocketAddress addr = (InetSocketAddress) netState.getSocketAddressOf(peer);
		int port = NetUtils.getDirectPort(peer.getID());
		InetSocketAddress conAddr = new InetSocketAddress(addr.getHostString(), port);
		SocketChannel channel = SocketChannel.open(conAddr);
		channel.configureBlocking(true);
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(netState.getLocalPeer().getID());
		buffer.flip();
		channel.write(buffer);
		
		buffer.clear();
		monitorDirectConnection(peer, channel);
	}
	/**
	 * Sends a message directly to the specified peer. This must be 
	 * preceded with ensureDirectConnection to avoid errors.
	 * @param peer the peer
	 */
	public void sendDirectly(Peer peer, ByteBuffer buffer) throws IOException {
		SocketChannel channel = getMonitoringChannel(peer);
		if(channel == null)
			throw new IllegalArgumentException("Call ensureDirectConnection first! (No direct channel with peer has been created)");
		
		channel.write(buffer);
	}
	
	/**
	 * Destroys all unnecessary connections caused from ensureDirectConnection
	 * 
	 * @throws IOException if an exception occurs
	 */
	public void destroyUnnecessaryConnections() throws IOException {
		List<Peer> connected = gameState.getConnectedPeers();
		
		synchronized(connected) {
			for(Peer peer : connected) {
				destroyUnnecessaryConnection(peer);
			}
		}
	}
	
	/**
	 * Destroys a specific unnecessary connection caused from ensureDirectConnection,
	 * and forgets the associated encryption key
	 * 
	 * @param peer the peer
	 * @throws IOException if an exception occurs
	 */
	public void destroyUnnecessaryConnection(Peer peer) throws IOException {
		SocketChannel channel = getMonitoringChannel(peer);
		if(channel == null)
			return;
		
		ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.DESTROYING_CHANNEL.getMaxPacketSize());
		buffer.putInt(PacketHeader.DESTROYING_CHANNEL.getValue());
		pManager.send(PacketHeader.DESTROYING_CHANNEL, buffer);
		buffer.flip();
		
		channel.write(buffer);
		stopMonitoring(peer);
	}
	
	/**
	 * Called when a direct connection is accepted
	 * @param channel
	 * @throws IOException
	 */
	protected void onConnectionAcception(SocketChannel channel)
			throws IOException {
		channel.configureBlocking(true);
		
		ByteBuffer buffer = ByteBuffer.allocate(4);
		
		List<Peer> peers = gameState.getConnectedPeers();
		InetSocketAddress channelAddr = (InetSocketAddress) channel.getRemoteAddress();
		InetSocketAddress peerAddr;
		
		Peer expected = null;
		synchronized(peers) {
			for(Peer peer : peers) { 
				peerAddr = (InetSocketAddress) netState.getSocketAddressOf(peer);
				if(NetUtils.addressMatches(peerAddr, channelAddr)) {
					expected = peer;
					break;
				}
			}
		}
		
		if(expected == null) {
			System.err.println("Direct connection was sent, but does not contain an expected address. Closing as quick as possible to prevent DDoS");
			channel.close();
			return;
		}
		channel.read(buffer);
		buffer.flip();
		
		int peerId = buffer.getInt();
		
		if(peerId != expected.getID()) {
			System.err.println("Direct connection was sent, but id does not match the expected id.");
			System.err.println("ID: " + peerId + ", Expected ID: " + expected.getID());
			System.err.println("--");
			System.err.println("Checking if that peer's address also matches (LAN connections can do this)");
			Peer peer = NetUtils.getPeerByID(peers, peerId);
			boolean killConnection = false;
			if(peer == null) {
				System.err.println("No peer found by that id, killing connection");
				killConnection = true;
			}
			else {
				peerAddr = (InetSocketAddress) netState.getSocketAddressOf(peer);
				if(NetUtils.addressMatches(peerAddr, channelAddr)) {
					System.err.println("Address matches, allowing the connection");
				}else {
					System.err.println("Address does NOT match, disallowing connection");
					System.err.println("  (That ID's Addr: '" + peerAddr.getHostName() + "' vs this connections '" + channelAddr.getHostName() + "'");
					killConnection = true;
				}
			}
			System.err.flush();
			if(killConnection) {
				channel.close();
				return;
			}
		}
		
		System.out.println("Peer directly connected: " + expected.getName());
		monitorDirectConnection(expected, channel);
	}
	
	/**
	 * 
	 * @param buffer the buffer
	 * @param from may or may not affect anything, based on the module. may or may not be allowed to be null
	 */
	public void handleRead(ByteBuffer buffer, Peer from) {
		while(buffer.hasRemaining()) {
			int peerId = buffer.getInt();
			int headerInt = buffer.getInt();

			Peer peer = NetUtils.getPeerByID(gameState.getConnectedPeers(), peerId);
			PacketHeader header = PacketHeader.byValue(headerInt);

			ParsedPacket parsed = PacketManager.instance.parse(header, buffer);
			PacketManager.instance.broadcastPacket(peer, parsed);
		}
	}
	
	protected void monitorDirectConnection(Peer peer, SocketChannel channel) {
		try {
			channel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		peer.metaData.put("directConnection", channel);
		dcmt.monitoring.add(peer);
	}
	
	protected void stopMonitoring(Peer peer) {
		peer.metaData.remove("directConnection");
	}
	
	protected SocketChannel getMonitoringChannel(Peer peer) {
		return peer.metaData.containsKey("directConnection") ? (SocketChannel) peer.metaData.get("directConnection") : null;
	}
}
