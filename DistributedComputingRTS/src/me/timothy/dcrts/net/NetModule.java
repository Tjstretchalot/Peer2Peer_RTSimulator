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
