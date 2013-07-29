package me.timothy.dcrts.net.packets;

import java.net.InetSocketAddress;
import java.util.Map;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class SendNetInfoPacket implements ParsedPacket {
	private Map<Integer, InetSocketAddress> peerAddresses;
	
	public SendNetInfoPacket(Map<Integer, InetSocketAddress> addresses) {
		peerAddresses = addresses;
	}
	
	public InetSocketAddress getAddress(int id) {
		return peerAddresses.get(id);
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.SEND_NET_INFO;
	}

}
