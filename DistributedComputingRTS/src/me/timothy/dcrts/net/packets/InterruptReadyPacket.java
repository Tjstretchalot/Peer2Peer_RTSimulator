package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class InterruptReadyPacket implements ParsedPacket {
	
	public InterruptReadyPacket() {
		
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.INTERRUPT_READY;
	}

}
