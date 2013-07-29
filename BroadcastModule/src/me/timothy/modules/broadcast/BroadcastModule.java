package me.timothy.modules.broadcast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import me.timothy.dcrts.net.NetModule;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.NetUtils;

/**
 * Broadcast Module has two main uses, a client-server like relationship
 * and the main networking module when the game is in progress<br>
 * <br>
 * Client-Server Relationship:<br>
 * <p>
 *   The broadcast module sends information to all other peers,
 *   who are running a listener module in this instance. The peers
 *   merely respond requests, rather than making any decisions for themself
 * </p><br>
 * 
 * Main Networking Module:<br>
 * <p>
 *   The broadcast module talks to all directly connected peers, which should
 *   either be NodeModules, ListenerModules, or BroadcastModules.
 *   
 *   <ul>
 *     <li>BroadcastModules - These may have extra communication to ensure syncage.</li>
 *     <li>NodeModules - These will relay information down that was sent from the broadcast module</li>
 *     <li>ListenerModules - These will simply respond to requests, either through a node module or directly with this broadcast module</li>
 *   </ul>
 * </p>
 * 
 * Metadata on peers:
 * <ol>
 *   <li>directlyConnected - Boolean, whether this peer is directly connected</li>
 *   <li>parentNode - Peer, the parent node, if this peer is not directly connected</oli>
 * </ol>
 * @author Timothy
 *
 */
public class BroadcastModule extends NetModule {
	private ReadThread readThread;
	private ByteBuffer copyBuffer;
	
	public BroadcastModule() {
		super();
		copyBuffer = ByteBuffer.allocate(PacketHeader.getLargestPacketSize());
	}
	
	@Override
	public void onActivate() {
		System.out.println("BroadcastModule activated!");
		readThread = new ReadThread(this, gameState, netState);
		readThread.start();
	}

	@Override
	public void onDeactivate() {
		System.out.println("BroadcastModule deactivated!");
		readThread.stopReading();
	}
	
	@Override
	public void handleRead(ByteBuffer buffer, Peer from) {
		int initSpot = -1, finishSpot = -1;
		while(buffer.hasRemaining()) { 
			initSpot = buffer.position();
			int peerId = buffer.getInt();
			int headerInt = buffer.getInt();
			
			Peer peer = NetUtils.getPeerByID(gameState.getConnectedPeers(), peerId);
			PacketHeader header = PacketHeader.byValue(headerInt);

			ParsedPacket parsed = PacketManager.instance.parse(header, buffer);
			PacketManager.instance.broadcastPacket(peer, parsed);
			finishSpot = buffer.position();
			
			buffer.position(initSpot);
			for(int i = 0; i < (finishSpot - initSpot); i++) {
				copyBuffer.put(buffer.get());
			}
			
			if(buffer.position() != finishSpot)
				throw new AssertionError("Buffer position does not match what it should be!");
			
			copyBuffer.flip();
			
			try {
				sendData(copyBuffer, from);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			System.out.println("Copied " + copyBuffer.position() + " bytes to other peers");
			copyBuffer.clear();
		}
	}

	@Override
	public void sendData(ByteBuffer buffer, Peer... except) throws IOException {
		int bufferStartPos = buffer.position();
		List<Peer> peers = gameState.getConnectedPeers();
		
		synchronized(peers) {
			for(Peer p : peers) {
				boolean run = true;
				for(Peer tmp : except) {
					if(tmp.equals(p)) {
						run = false;
						break;
					}
				}
				if(!run)
					continue;
				handleMeta(p);
				if((boolean) p.metaData.get("directlyConnected")) {
					SocketChannel sc = netState.getSocketChannelOf(p);
					if(sc == null) {
						System.err.println("No socket channel detected for supposedly directly connected peer '" + p.getName() + "' (" + p.getID() + ")");
						continue;
					}
					buffer.position(bufferStartPos);
					sc.write(buffer);
				}else {
					System.err.println("Peer is not directly connected, DOING NOTHING!");
				}
			}
		}
	}
	/**
	 * Handles defaulting metadata values for a peer, to avoid
	 * null pointer exceptions
	 * @param peer the peer
	 */
	protected void handleMeta(Peer peer) {
		if(!peer.metaData.containsKey("directlyConnected")) {
			if(netState.getSocketChannelOf(peer) != null)
				peer.metaData.put("directlyConnected", true);
		}
	}
}
