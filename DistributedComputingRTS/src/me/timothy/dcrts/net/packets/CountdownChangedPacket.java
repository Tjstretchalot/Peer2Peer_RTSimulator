package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class CountdownChangedPacket implements ParsedPacket {
	
	private int nCountdown;
	
	public CountdownChangedPacket(int nCountdown) {
		this.nCountdown = nCountdown;
	}
	
	public int getCountdown() {
		return nCountdown;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.SYNC_COUNTDOWN;
	}

}
