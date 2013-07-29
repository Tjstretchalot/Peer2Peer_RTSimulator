package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;

/**
 * Time sent is based on the original packet, not the more recent time
 * @author Timothy
 *
 */
public class ReturnPingPacket extends PingPacket {

	public ReturnPingPacket(long timeSent) {
		super(timeSent);
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.RETURN_PING;
	}

}
