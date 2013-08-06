package me.timothy.modules.midnode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import me.timothy.dcrts.net.NetModule;
import me.timothy.dcrts.peer.Peer;

/**
 * The middle-man, which relays information up to at most
 * one directly connected peer, which will handle it as 
 * per it's net handler. It will relay information down 
 * to an unlimited number of peers, although no more than 5
 * is expected.
 * 
 * To initialize a midnode module, the some metadata must be made
 * to explain to the midnode module what you are trying to do. Specifically,
 * this is the procedure to set 'Host' to the peer above you and 'ClientA', 'ClientB',
 * and 'ClientC' below you. 'ClientC' has two children, 'ClientD' and 'ClientE'
 * 
 * <pre>
 * loop through every peer
 *   clear metadata values 'abovePeer', 'belowPeer', 'directlyConnected', and 'parent'
 *   if peer is not 'ClientA', 'ClientB' or 'ClientC' 
 *     if peer is 'ClientD' or 'ClientE'
 *       set 'belowPeer' to true
 *       set 'directlyConnected' to false
 *       set 'parent' to 'ClientC' (Peer object)
 *     else
 *       set 'abovePeer' to true
 *     end if
 *   else 
 *     set 'belowPeer' to true
 *     set 'directlyConnected' to true
 *   end if
 * end loop
 * </pre>
 * 
 * There should be no metadata on peers that are not either
 * below this peer, or directly above this peer, that is 
 * specified above
 * 
 * @author Timothy
 */
public class MidnodeModule extends NetModule {

	public MidnodeModule() {}

	@Override
	public void onActivate() {
		super.onActivate();

		verifyMetadata();
	}


	@Override
	public void sendData(ByteBuffer buffer, Peer... except) throws IOException {

	}

	/**
	 * Verifies that all current metadata is valid
	 */
	protected void verifyMetadata() {
		List<Peer> peers = gameState.getConnectedPeers();

		boolean foundAbovePeer = false;
		List<Peer> mem = new ArrayList<>();

		synchronized(peers) {
			for(Peer p : peers) {
				if(p.metaData.containsKey("abovePeer")) {
					assert !foundAbovePeer : "Multiple peers with 'abovePeer' specified";
				foundAbovePeer = true;

				assert !p.metaData.containsKey("belowPeer") : "Peer with 'abovePeer' also has 'belowPeer'";
				assert !p.metaData.containsKey("parent") : "Peer with 'parent' specified when he is above us";
				assert p.metaData.containsKey("directlyConnected") : "Unnecessary information on peer above us but not directly connected";
				continue;
				}

				if(p.metaData.containsKey("belowPeer")) {
					boolean direct = p.metaData.containsKey("directlyConnected");

					if(direct) {
						assert !p.metaData.containsKey("parent") : "Directly connected peer has parent specified";
					}else {
						assert p.metaData.containsKey("parent") : "Peer below but not directly connected has no parent specified";

						Object parentPeerObj = p.metaData.get("parent");

						assert parentPeerObj != null : "Parent peer object cannot be null if the peer is not directly connected";
						assert parentPeerObj instanceof Peer : "Parent object should be an instance of Peer";

						Peer peer = (Peer) parentPeerObj;
						mem.add(peer);
						while(!peer.metaData.containsKey("directlyConnected")) {
							parentPeerObj = peer.metaData.get("parent");

							assert parentPeerObj != null : "Parent peer in chain does not have a parent object but is not directly connected";
							assert parentPeerObj instanceof Peer : "Parent peer in chain is not an instance of peer";

							peer = (Peer) parentPeerObj;
							if(mem.contains(peer)) 
								assert false : "Looped parent peers in chain: " + mem + " followed by " + peer;
							mem.add(peer);
						}
						mem.clear();
					}
					continue;
				}
				
				assert !p.metaData.containsKey("parent") : "Non-related peer has parent key";
				assert !p.metaData.containsKey("directlyConnected") : "Non-related peer is directly connected?";
			}
		}
	}
}
