package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class SetReadyPacket implements ParsedPacket {
	
	private boolean ready;
	
	public SetReadyPacket(boolean ready) {
		this.ready = ready;
	}
	
	public boolean ready() {
		return ready;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.SET_READY;
	}

}
