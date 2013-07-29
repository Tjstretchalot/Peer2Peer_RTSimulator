package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class ConnectPacket implements ParsedPacket {
	private String name;
	private int id;
	private boolean idAssigned;
	private boolean ready;
	
	public ConnectPacket(String name, boolean idAssigned, boolean ready, int id) {
		this.name = name;
		this.id = id;
		this.idAssigned = idAssigned;
		this.ready = ready;
	}

	@Override
	public PacketHeader getHeader() {
		return PacketHeader.CONNECT;
	}
	
	public String getName() {
		return name;
	}

	public boolean idAssigned() {
		return idAssigned;
	}
	
	public int getID() {
		return id;
	}
	
	public boolean ready() {
		return ready;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (idAssigned ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ConnectPacket other = (ConnectPacket) obj;
		if (id != other.id)
			return false;
		if (idAssigned != other.idAssigned)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
