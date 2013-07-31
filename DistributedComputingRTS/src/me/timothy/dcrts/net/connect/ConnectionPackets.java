package me.timothy.dcrts.net.connect;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.timothy.dcrts.net.module.ModuleHandler;
import me.timothy.dcrts.net.packets.ChangeModulePacket;
import me.timothy.dcrts.net.packets.DestroyingChannelPacket;
import me.timothy.dcrts.net.packets.WhisperPacket;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.NetUtils;

/**
 * Handles all of the packets for the connection area and
 * connector.
 * 
 * @author Timothy
 */
public class ConnectionPackets implements PacketListener {
	private static ConnectionPackets instance;

	private ConnectionPackets() {

	}

	/**
	 * Initialize all packet listeners and parsers for
	 * the connection
	 */
	public static void init() {
		if(instance != null)
			return;
		instance = new ConnectionPackets();
		
		PacketManager pm = PacketManager.instance;
		pm.registerPacketParser(instance, "parseChangeModule", PacketHeader.CHANGE_MODULE);
		pm.registerPacketParser(instance, "parseWhisper", PacketHeader.WHISPER);
		pm.registerPacketParser(instance, "parseDestroyChannel", PacketHeader.DESTROYING_CHANNEL);
		
		pm.registerPacketSender(instance, "createChangeModule", PacketHeader.CHANGE_MODULE);
		pm.registerPacketSender(instance, "createWhisper", PacketHeader.WHISPER);
		pm.registerPacketSender(instance, "createDestroyChannel", PacketHeader.DESTROYING_CHANNEL);
	}
	
	/**
	 * Parses a change module packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the parsed packet
	 */
	public ParsedPacket parseChangeModule(PacketHeader header, ByteBuffer buffer) {
		int peerId = buffer.getInt();
		
		boolean netMod = buffer.get() == 1;
		
		int mLen = buffer.getInt();
		String module = NetUtils.readString(buffer, mLen);
		int sha1HashLen = buffer.getInt(); // 20
		
		byte[] hash = new byte[sha1HashLen];
		buffer.get(hash);
		
		return new ChangeModulePacket(peerId, module, netMod, hash);
	}

	/**
	 * Writes a change module packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments Length 3, contains a peer (whose changing the module), if the module is an et module,
	 * 				and a string (the name of the module that is being changed to)
	 */
	public void createChangeModule(ByteBuffer buffer, Object... arguments) {
		Peer peer = (Peer) arguments[0];
		boolean netMod = (boolean) arguments[1];
		String module = (String) arguments[2];
		
		byte[] sha1Hash = null;
		try {
			sha1Hash = NetUtils.sha1Hash(ModuleHandler.getFileForModule(module));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		buffer.putInt(peer.getID());
		buffer.put(netMod ? (byte) 1 : (byte) 0);
		buffer.putInt(module.length());
		NetUtils.putString(buffer, module);
		buffer.putInt(sha1Hash.length);
		buffer.put(sha1Hash);
	}
	
	/**
	 * Parses a whisper packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the parsed packet
	 */
	public ParsedPacket parseWhisper(PacketHeader header, ByteBuffer buffer) {
		int pId = buffer.getInt();
		int msgLen = buffer.getInt();
		String msg = NetUtils.readString(buffer, msgLen);
		
		return new WhisperPacket(pId, msg);
	}
	
	/**
	 * Writes a whisper packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments Length 2 (Peer that it is being sent to, message that is being sent)
	 */
	public void createWhisper(ByteBuffer buffer, Object... arguments) {
		Peer peer = (Peer) arguments[0];
		String msg = (String) arguments[1];
		int len = msg.length();
		
		buffer.putInt(peer.getID());
		buffer.putInt(len);
		NetUtils.putString(buffer, msg);
	}
	
	/**
	 * Parses a destroy channel packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return a DestroyingChannelPacket
	 */
	public ParsedPacket parseDestroyChannel(PacketHeader header, ByteBuffer buffer) {
		return new DestroyingChannelPacket();
	}
	
	/**
	 * Writes a destroy channel packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments none
	 */
	public void createDestroyChannel(ByteBuffer buffer, Object... arguments) {
		
	}
}
