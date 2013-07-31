package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class DirectConnectionPacket implements ParsedPacket {
	private ParsedPacket realPacket;
	
	public DirectConnectionPacket(ParsedPacket real) {
		realPacket = real;
	}
	
	public ParsedPacket getPacket() {
		return realPacket;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.DIRECT_PACKET;
	}

}
