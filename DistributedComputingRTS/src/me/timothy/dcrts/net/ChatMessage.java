package me.timothy.dcrts.net;

import me.timothy.dcrts.peer.Peer;

public class ChatMessage {
	private String message;
	private Peer sender;
	private long timeRecieved;
	
	public ChatMessage(Peer peer, String msg) {
		this.message = msg;
		this.sender = peer;
		timeRecieved = System.currentTimeMillis();
	}

	public String getMessage() {
		return message;
	}

	public Peer getSender() {
		return sender;
	}
	
	public long getTimeRecieved() {
		return timeRecieved;
	}
}
