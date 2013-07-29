package me.timothy.dcrts.packet;


/**
 * A packet header contains information about the 'head', or first
 * four bytes of, a packet. These 4 bytes declare which packet type
 * it is, and thus which packetparser should parse the packet.
 * 
 * @author Timothy
 */
public enum PacketHeader {
	ANY(-2, 0), // This header solely means you want to respond to all headers, and is *never* sent over the net
	ERROR(-1, 256), // if you get this, you should probably just jump off a roof.
	
	// LOBBY PACKETS 
	PING(1, 18), // just an average ping. This can be called to any connected peer at any time
	RETURN_PING(2, 18), // what gets returned when you ping someone
	CONNECT(3, 256), // the first packet that should be sent after connecting. Can be sent as is to all players after an id is negotiated
	ASSIGN_ID(4, 18), // assign an id to a peer
	DISCONNECT(5, 256), // the packet to signal a graceful disconnect. Can be sent as is to all players
	BEGIN_PING(6, 12), // a way to tell the host/broadcaster that you want to ping someone you aren't directly connected with
	CHANGE_NAME(7, 256), // signals a name change 
	UPDATE_SETTINGS(8, 256), // update game settings
	SET_READY(9, 9), // set yourself to ready/not ready
	BEGIN_GAME(10, 8), // begin the game!
	SYNC_COUNTDOWN(11, 13), // get the current count-down ms.
	BEGIN_COUNTDOWN(12, 8), // begin the count-down
	INTERRUPT_READY(13, 8), // interrupt count-down
	
	SEND_NET_INFO(14, 256),
	// END LOBBY PACKETS
	
	ALL_CHAT(15, 512),
	WHISPER(16, 512), 
	
	CHANGE_MODULE(17, 128) // marks a change in a peers module
	
	;
	private static int largest = -1;
	
	private int value;
	private int maxPacketSize;

	PacketHeader(int value, int maxPacketSize) {
		this.value = value;
		this.maxPacketSize = maxPacketSize;
	}

	public int getValue() {
		return value;
	}
	
	public int getMaxPacketSize() {
		return maxPacketSize;
	}

	public static PacketHeader byValue(int header) {
		PacketHeader[] arr = values();
		for(PacketHeader ph : arr) {
			if(ph.value == header)
				return ph;
		}
		return null;
	}
	
	public static int getLargestPacketSize() {
		if(largest != -1)
			return largest;
		
		PacketHeader[] arr = values();
		for(PacketHeader ph : arr) {
			if(ph.maxPacketSize > largest)
				largest = ph.maxPacketSize;
		}
		return largest;
	}
}
