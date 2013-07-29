package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class ErrorPacket implements ParsedPacket {

	private String reason;
	
	public ErrorPacket(String reason) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.ERROR;
	}

}
