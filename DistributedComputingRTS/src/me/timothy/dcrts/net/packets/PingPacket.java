package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class PingPacket implements ParsedPacket {
	private long timeSent;
	
	public PingPacket(long timeSent) {
		this.timeSent = timeSent;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.PING;
	}

	public long getTimeSent() {
		return timeSent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timeSent ^ (timeSent >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PingPacket other = (PingPacket) obj;
		if (timeSent != other.timeSent)
			return false;
		return true;
	}
	
	
}
