package me.timothy.dcrts.peer;

import me.timothy.dcrts.utils.NetUtils;

public class PartialPeer extends Peer {
	
	public PartialPeer() {
		id = NetUtils.RESERVED_ID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		PartialPeer other = (PartialPeer) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PartialPeer [name=" + name + ", id=" + id + "]";
	}
}
