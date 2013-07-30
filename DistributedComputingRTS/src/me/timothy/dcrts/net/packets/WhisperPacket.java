package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class WhisperPacket implements ParsedPacket {
	private int peerId;
	private String message;
	
	public WhisperPacket(int peerId, String message) {
		super();
		this.peerId = peerId;
		this.message = message;
	}

	public int getPeerId() {
		return peerId;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public PacketHeader getHeader() {
		return PacketHeader.WHISPER;
	}

}
