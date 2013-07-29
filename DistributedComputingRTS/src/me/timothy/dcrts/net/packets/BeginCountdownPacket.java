package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class BeginCountdownPacket implements ParsedPacket {

	public BeginCountdownPacket() {
		
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.BEGIN_COUNTDOWN;
	}

}
