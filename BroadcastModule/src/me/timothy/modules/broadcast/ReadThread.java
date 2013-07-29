package me.timothy.modules.broadcast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import me.timothy.dcrts.GameState;
import me.timothy.dcrts.net.NetModule;
import me.timothy.dcrts.net.NetState;
import me.timothy.dcrts.peer.Peer;

/**
 * Basic implementation of a thread that iteratively reads from
 * a list of connections if they are directly connected.
 * 
 * @author Timothy
 */
public class ReadThread extends Thread {
	/**
	 * Amount of milliseconds to sleep if nothing is read
	 */
	public static final long DELAY_ON_NO_READ = 1;
	
	private NetModule netModule;
	private NetState netState;
	private GameState gameState;
	private volatile boolean running;
	
	public ReadThread(NetModule netModule, GameState gState, NetState nState) {
		this.netModule = netModule;
		gameState = gState;
		netState = nState;
	}
	
	public void stopReading() {
		running = false;
	}
	
	@Override
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024); // 1kb max packet size is plenty
		int nRead = -1;
		boolean readSomething = false;
		List<Peer> peers;
		while(running) {
			peers = gameState.getConnectedPeers();
			synchronized(peers) {
				for(Peer p : peers) {
					if(p.metaData.containsKey("directlyConnected") && (boolean) p.metaData.get("directlyConnected")) {
						SocketChannel sc = netState.getSocketChannelOf(p);
						
						try {
							nRead = sc.read(buffer);
						} catch (IOException e) {
							nRead = 0;
							e.printStackTrace();
						}
						
						if(nRead == 0)
							continue;
						readSomething = true;
						buffer.flip();
						netModule.handleRead(buffer, p);
						buffer.clear();
					}
				}
			}
			
			if(!readSomething) {
				try {
					Thread.sleep(DELAY_ON_NO_READ);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
				readSomething = false;
			}
		}
	}
}
