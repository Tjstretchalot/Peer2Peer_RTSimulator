package me.timothy.dcrts.net;

import me.timothy.dcrts.net.lobby.Lobby;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.state.LobbyState;

public class PingingThread extends Thread implements PacketListener {
	/**
	 * How long should we constantly ping for?
	 */
	public static final long PING_TIME_MS = 5000;
	
	private LobbyState lobbyState;
	private Lobby lobby;
	
	private volatile boolean pingResponse;
	
	public PingingThread(LobbyState state, Lobby lob) {
		lobbyState = state;
		lobby = lob;
	}
	
	@Override
	public void run() {
		long start = System.currentTimeMillis();
		int numPings = 0;
		boolean noResponse = false;
		
		while(System.currentTimeMillis() - start < PING_TIME_MS) {
			lobby.ping();
			while(!pingResponse) {
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
					noResponse = true;
					break;
				}
			}
			pingResponse = false;
			numPings++;
		}
		long end = System.currentTimeMillis();
		if(noResponse) {
			lobbyState.setPingTime(999);
		}else {
			long totalTime = end - start;
			System.out.println("Ping done, completed " + numPings + " pings in " + totalTime + "ms");
			long average = Math.round((double) totalTime / numPings);
			lobbyState.setPingTime(average);
		}
	}
	
	@PacketHandler(header=PacketHeader.RETURN_PING, priority=3)
	public void onPing(Peer peer, ParsedPacket packet) {
		pingResponse = true;
	}
}
