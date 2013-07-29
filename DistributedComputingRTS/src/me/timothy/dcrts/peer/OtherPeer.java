package me.timothy.dcrts.peer;

public class OtherPeer extends Peer {
	
	
	public OtherPeer(String name, int id) {
		super.name = name;
		super.id = id;
	}
	
	public OtherPeer(PartialPeer partPeer) {
		this(partPeer.getName(), partPeer.getID());
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
		OtherPeer other = (OtherPeer) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OtherPeer [name=" + name + ", id=" + id + "]";
	}
}
