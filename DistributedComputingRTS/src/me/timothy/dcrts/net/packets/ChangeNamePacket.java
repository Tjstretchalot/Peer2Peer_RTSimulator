package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class ChangeNamePacket implements ParsedPacket {

	private String newName;
	
	public ChangeNamePacket(String newName){ 
		this.newName = newName;
	}
	
	public String getNewName() {
		return newName;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.CHANGE_NAME;
	}

}
