package me.timothy.dcrts.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.ErrorUtils;

/**
 * Represents a peer-to-peer model of networking
 * @author Timothy
 */
public class NetState {
	/**
	 * Internal representation of the net status of a peer
	 * @author Timothy
	 */
	private class NetInfo {
		SocketAddress address;
		SocketChannel channel;
		NetModule netModule;
		LogicModule logicModule;
		List<Peer> connected;
		
		/**
		 * Creates a net info
		 * @param addr address, may not be null
		 * @param chan channel, may be null
		 * @param netMod net module currently, may not be null
		 * @param logMod logic module currently, may not be null
		 * @param connectedWith who the peer is connected with, may not be null. 
		 */
		public NetInfo(SocketAddress addr, SocketChannel chan, NetModule netMod, LogicModule logMod, List<Peer> connectedWith) {
			if(netMod == null || logMod == null || connectedWith == null)
				ErrorUtils.nullPointer(new String[] { "netMod", "logMod", "connectedWith" }, netMod, logMod, connectedWith);
			
			address = addr;
			channel = chan;
			netModule = netMod;
			logicModule = logMod;
			
			connected = connectedWith;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((address == null) ? 0 : address.hashCode());
			result = prime * result
					+ ((channel == null) ? 0 : channel.hashCode());
			result = prime * result
					+ ((logicModule == null) ? 0 : logicModule.hashCode());
			result = prime * result
					+ ((netModule == null) ? 0 : netModule.hashCode());
			result = prime * result
					+ ((connected == null) ? 0 : connected.hashCode());
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
			NetInfo other = (NetInfo) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			if (channel == null) {
				if (other.channel != null)
					return false;
			} else if (!channel.equals(other.channel))
				return false;
			if (logicModule == null) {
				if (other.logicModule != null)
					return false;
			} else if (!logicModule.equals(other.logicModule))
				return false;
			if (netModule == null) {
				if (other.netModule != null)
					return false;
			} else if (!netModule.equals(other.netModule))
				return false;
			if(connected == null) {
				if(other.connected != null)
					return false;
			}else if(!connected.equals(other.connected))
				return false;
			
			return true;
		}

		private NetState getOuterType() {
			return NetState.this;
		}
	}
	
	private Map<Peer, NetInfo> peerInfo;
	private Peer localPeer;
	private NetInfo localInfo;
	
	public NetState() {
		peerInfo = new HashMap<>();
	}
	
	public NetModule getNetTypeOf(Peer peer) {
		return peer == localPeer ? localInfo.netModule : peerInfo.get(peer).netModule;
	}
	
	public LogicModule getLogicTypeOf(Peer peer) {
		return peer == localPeer ? localInfo.logicModule : peerInfo.get(peer).logicModule;
	}
	
	public SocketAddress getSocketAddressOf(Peer peer) {
		return peer == localPeer ? localInfo.address : peerInfo.get(peer).address;
	}
	
	public SocketChannel getSocketChannelOf(Peer peer) {
		return peer == localPeer ? localInfo.channel : peerInfo.get(peer).channel;
	}
	
	/**
	 * Will do necessary work of deactivating/activating if the peer
	 * is the local peer
	 * @param peer the peer
	 * @param netModule the net module
	 */
	public void setNetModuleOf(Peer peer, NetModule netModule) {
		if(netModule == null)
			ErrorUtils.nullPointer(new String[] { "netModule" }, netModule);
		if(peer.equals(localPeer)) {
			localInfo.netModule.onDeactivate();
			localInfo.netModule = netModule;
			netModule.onActivate();
			return;
		}
		peerInfo.get(peer).netModule = netModule;
	}
	
	/**
	 * Will do necessary work of deactivating/activating if the peer
	 * is the local peer
	 * @param peer the peer
	 * @param netModule the net module
	 */
	public void setLogicTypeOf(Peer peer, LogicModule logicModule) {
		if(logicModule == null)
			ErrorUtils.nullPointer(new String[] { "logicModule" }, logicModule);
		if(peer.equals(localPeer)) {
			localInfo.logicModule.onDeactivate();
			localInfo.logicModule = logicModule;
			localInfo.logicModule.onActivate();
			return;
		}
		peerInfo.get(peer).logicModule = logicModule;
	}
	
	// changing the address would be better off by making a new peer.
	
	public void setSocketChannelOf(Peer peer, SocketChannel channel) {
		if(peer == localPeer)
			throw new AssertionError("...wut? (attempt to set the socket channel of local peer)");
		peerInfo.get(peer).channel = channel;
	}
	
	/**
	 * Checks if a peer is registered in the net state. If this
	 * method is false, future calls to getXXX will throw an NPE,
	 * true will not throw NPE
	 * 
	 * @param peer the peer
	 * @return if that peer is registered
	 */
	public boolean isRegistered(Peer peer) {
		return peerInfo.containsKey(peer);
	}
	
	/**
	 * Registers a peer with a direct, active connection into the current
	 * net state
	 * @param peer the peer
	 * @param channel the channel
	 * @param netMod the network tactic
	 * @param logMod the logic strategy
	 * @param connectedWith who the peer is connected with. This array will be copied
	 * @throws RuntimeException if channel.getRemoteAddress() throws an IOException
	 */
	public void registerPeer(Peer peer, SocketChannel channel, NetModule netMod, LogicModule logMod, List<Peer> connectedWith) 
				throws RuntimeException {
		if(channel == null)
			ErrorUtils.nullPointer(new String[] { "channel", "netMod", "logMod" }, channel, netMod, logMod);
		try {
			List<Peer> connected = new ArrayList<>();
			for(Peer p : connectedWith) {
				if(!p.equals(peer))
					connected.add(p);
			}
			peerInfo.put(peer, new NetInfo(channel.getRemoteAddress(), channel, netMod, logMod, connected));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Registers a peer that does not have a direct, active connection into the current
	 * net state
	 * @param peer the peer
	 * @param address the address
	 * @param netMod the net module
	 * @param logMod the logic module
	 * @param connectedWith the list of peers {@code peer} is connected with. This array will be copied
	 */
	public void registerPeer(Peer peer, SocketAddress address, NetModule netMod, LogicModule logMod, List<Peer> connectedWith) {
		if(address == null)
			ErrorUtils.nullPointer(new String[] { "address", "netMod", "logMod" }, address, netMod, logMod);
		List<Peer> connected = new ArrayList<>();
		for(Peer p : connectedWith) {
			if(!p.equals(peer))
				connected.add(p);
		}
		peerInfo.put(peer, new NetInfo(address, null, netMod, logMod, connected));
	}
	
	/**
	 * Removes the specified peer from the net state. If
	 * the peer is not registered, no errors are thrown
	 * and nothing is changed.
	 * 
	 * @param peer the peer to unregister
	 */
	public void unregisterPeer(Peer peer) {
		peerInfo.remove(peer);
	}
	
	public Peer getLocalPeer() {
		return localPeer;
	}
	
	public NetModule getLocalNetModule() {
		return localInfo.netModule;
	}
	
	public LogicModule getLocalLogicModule() {
		return localInfo.logicModule;
	}
	
	public void setLocalPeer(Peer p, NetModule netMod, LogicModule logMod, List<Peer> connectedWith) {
		localPeer = p;
		localInfo = new NetInfo(null, null, netMod, logMod, connectedWith);
	}

	public List<Peer> getPeersWithNetModule(String string) {
		List<Peer> res = new ArrayList<>();
		
		if(getLocalNetModule().getName().equals(string))
			res.add(localPeer);
		
		Set<Peer> keys = peerInfo.keySet();
		for(Peer p : keys) {
			if(p == null || getNetTypeOf(p) == null)
				ErrorUtils.nullPointer(new String[] { "p", "getNetTypeOf(p)" }, p, p != null ? getNetTypeOf(p) : null);
			if(getNetTypeOf(p).getName().equals(string))
				res.add(p);
		}
		
		return res;
	}
	
	public List<Peer> getPeersWithLogicModule(String string) {
		List<Peer> res = new ArrayList<>();
		
		if(getLocalLogicModule().getName().equals(string))
			res.add(localPeer);
		
		Set<Peer> keys = peerInfo.keySet();
		for(Peer p : keys) {
			if(getLogicTypeOf(p).getName().equals(string))
				res.add(p);
		}
		
		return res;
	}

	/**
	 * Gets the list of peers to which the specified peer
	 * is connected
	 * @param p the peer
	 * @return who he or she's connected with
	 */
	public List<Peer> getConnections(Peer p) {
		if(p.equals(localPeer))
			return getLocalConnections();
		return peerInfo.get(p).connected;
	}
	
	public List<Peer> getLocalConnections() {
		return localInfo.connected;
	}
}
