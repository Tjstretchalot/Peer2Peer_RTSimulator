package me.timothy.modules.listener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import me.timothy.dcrts.net.NetModule;
import me.timothy.dcrts.net.packets.ChangeModulePacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;
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
		
		reallyConnected = netState.getPeersWithNetModule("BroadcastModule").get(0);
		readThread = new ReadThread(this, netState.getSocketChannelOf(reallyConnected));
		readThread.start();
		pManager.registerClass(this);

		System.out.println("ListenerModule Activated");
	}

	@Override
	public void onDeactivate() {
		readThread.stopGracefully();

		pManager.unregisterClass(this);
		System.out.println("ListenerModule Deactivated");
	}

	@Override
	public void sendData(ByteBuffer buffer, Peer... except) throws IOException {
		SocketChannel sc = netState.getSocketChannelOf(reallyConnected);

		sc.write(buffer);
	}

	@PacketHandler(header=PacketHeader.CHANGE_MODULE, priority=10)
	public void onChangeModule(Peer peer, ParsedPacket packet) {
			if(!peer.equals(reallyConnected))
				return;
			
			// oh man oh man oh man
			ChangeModulePacket cmp = (ChangeModulePacket) packet;
			if(cmp.isNetModule() && !cmp.getModule().equals("BroadcastModule")) {
				List<Peer> broadcasters = netState.getPeersWithNetModule("BroadcastModule");
				if(broadcasters.size() == 0) {
					throw new IllegalArgumentException("No broadcasters to really be connected to!");
				}
				reallyConnected = broadcasters.get(0);
				System.out.println("Changing really connected peer!");
			}
	}

	@Override
	public void ensureDirectConnection(Peer peer) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendDirectly(Peer peer) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroyUnnecessaryConnections() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroyUnnecessaryConnection(Peer peer) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
