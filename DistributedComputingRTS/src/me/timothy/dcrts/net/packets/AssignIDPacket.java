package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

/**
 * The packet related to {@link PacketHeader#ASSIGN_ID}. ID is always
 * -1 if requestingID is true
 * 
 * @author Timothy
 */
public class AssignIDPacket implements ParsedPacket {
	private boolean requestingID;
	private int id;
	private int peerID;
	
	public AssignIDPacket(boolean requestingID, int id, int peerID) {
		this.requestingID = requestingID;
		this.id = id;
		this.peerID = peerID;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.ASSIGN_ID;
	}

	public boolean requestingID() {
		return requestingID;
	}
	
	public int getID() {
		return id;
	}
	
	public int getPeerID() {
		return peerID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (requestingID ? 1231 : 1237);
		result = prime * result + peerID;
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
		AssignIDPacket other = (AssignIDPacket) obj;
		if (id != other.id)
			return false;
		if (requestingID != other.requestingID)
			return false;
		if(peerID != other.peerID)
			return false;
		return true;
	}
	
	
}
