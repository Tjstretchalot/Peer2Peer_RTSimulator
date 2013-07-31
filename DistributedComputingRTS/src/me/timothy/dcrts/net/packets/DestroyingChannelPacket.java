package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class DestroyingChannelPacket implements ParsedPacket {

	@Override
	public PacketHeader getHeader() {
		return PacketHeader.DESTROYING_CHANNEL;
	}

}
