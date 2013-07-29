package me.timothy.modules.listener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import me.timothy.dcrts.net.NetModule;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.ErrorUtils;

/**
 * Very basic listening module, or effectively an end-node module. This
 * node should have one seriously connected peer, and should never have 
 * to send information outside of that peer.
 * 
 * @author Timothy
 */
public class ListenerModule extends NetModule {
	private Peer reallyConnected;
	private ReadThread readThread;
	@Override
	public void onActivate() {
		if(netState == null || gameState == null || gameState.getConnectedPeers() == null) {
			ErrorUtils.nullPointer(new String[] { "netState", "gameState", "gameState.getConnectedPeers()" }, netState, 
					gameState, (gameState != null ? gameState.getConnectedPeers() : null));
		}
		
		reallyConnected = gameState.getConnectedPeers().get(0);
		readThread = new ReadThread(this, netState.getSocketChannelOf(reallyConnected));
		readThread.start();

		System.out.println("ListenerModule Activated");
	}

	@Override
	public void onDeactivate() {
		readThread.stopGracefully();

		System.out.println("ListenerModule Deactivated");
	}

	@Override
	public void sendData(ByteBuffer buffer, Peer... except) throws IOException {
		SocketChannel sc = netState.getSocketChannelOf(reallyConnected);

		sc.write(buffer);
	}

}
