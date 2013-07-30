package me.timothy.dcrts.net;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.timothy.dcrts.packet.PacketHeader;
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
	 * The peer then exchanges encryption keys to be utilized until the next call
	 * to destroyUnnecessaryConnections() or destroyUnnecessaryConnection#Peer
	 * @param peer the peer 
	 */
	public abstract void ensureDirectConnection(Peer peer) throws IOException;
	/**
	 * Sends a message directly to the specified peer. This must be 
	 * preceded with ensureDirectConnection to avoid errors.
	 * 
	 * This will be encrypted
	 * 
	 * @param peer the peer
	 */
	public abstract void sendDirectly(Peer peer) throws IOException;
	
	/**
	 * Destroys any unnecessary connections caused from ensureDirectConnection, and 
	 * 'kills' encryption keys.
	 * 
	 * @throws IOException if an exception occurs
	 */
	public abstract void destroyUnnecessaryConnections() throws IOException;
	
	/**
	 * Destroys a specific unnecessary connection caused from ensureDirectConnection,
	 * and forgets the associated encryption key
	 * 
	 * @param peer the peer
	 * @throws IOException if an exception occurs
	 */
	public abstract void destroyUnnecessaryConnection(Peer peer) throws IOException;
	
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
}
