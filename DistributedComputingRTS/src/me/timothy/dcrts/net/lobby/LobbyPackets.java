package me.timothy.dcrts.net.lobby;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.timothy.dcrts.net.packets.AssignIDPacket;
import me.timothy.dcrts.net.packets.BeginCountdownPacket;
import me.timothy.dcrts.net.packets.ChangeNamePacket;
import me.timothy.dcrts.net.packets.ConnectPacket;
import me.timothy.dcrts.net.packets.CountdownChangedPacket;
import me.timothy.dcrts.net.packets.DisconnectPacket;
import me.timothy.dcrts.net.packets.ErrorPacket;
import me.timothy.dcrts.net.packets.InterruptReadyPacket;
import me.timothy.dcrts.net.packets.PingPacket;
import me.timothy.dcrts.net.packets.ReturnPingPacket;
import me.timothy.dcrts.net.packets.SendNetInfoPacket;
import me.timothy.dcrts.net.packets.SetReadyPacket;
import me.timothy.dcrts.net.packets.UpdateSettingsPacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.PacketManager;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.settings.GameSettingsBuilder;
import me.timothy.dcrts.utils.NetUtils;

/**
 * Handles initializing all the packet parsers and listeners for the lobby
 * 
 * @author Timothy
 */
public class LobbyPackets implements PacketListener {
	private static LobbyPackets instance;

	private LobbyPackets() {

	}

	/**
	 * Initialize all packet listeners and parsers for
	 * the lobby. 
	 */
	public static void init() {
		if(instance != null)
			return;
		instance = new LobbyPackets();

		// init parsers
		PacketManager pm = PacketManager.instance;
		pm.registerPacketParser(instance, "parsePing", PacketHeader.PING);
		pm.registerPacketParser(instance, "parseReturnPing", PacketHeader.RETURN_PING);
		pm.registerPacketParser(instance, "parseConnect", PacketHeader.CONNECT);
		pm.registerPacketParser(instance, "parseAssignId", PacketHeader.ASSIGN_ID);
		pm.registerPacketParser(instance, "parseDisconnect", PacketHeader.DISCONNECT);
		pm.registerPacketParser(instance, "parseUpdateSettings", PacketHeader.UPDATE_SETTINGS);
		pm.registerPacketParser(instance, "parseSetReady", PacketHeader.SET_READY);
		pm.registerPacketParser(instance, "parseInterruptReady", PacketHeader.INTERRUPT_READY);
		pm.registerPacketParser(instance, "parseBeginCountdown", PacketHeader.BEGIN_COUNTDOWN);
		pm.registerPacketParser(instance, "parseSyncCountdown", PacketHeader.SYNC_COUNTDOWN);
		pm.registerPacketParser(instance, "parseError", PacketHeader.ERROR);
		pm.registerPacketParser(instance, "parseChangeName", PacketHeader.CHANGE_NAME);
		pm.registerPacketParser(instance, "parseSendNetInfo", PacketHeader.SEND_NET_INFO);

		pm.registerPacketSender(instance, "createPing", PacketHeader.PING);
		pm.registerPacketSender(instance, "createReturnPing", PacketHeader.RETURN_PING);
		pm.registerPacketSender(instance, "createConnect", PacketHeader.CONNECT);
		pm.registerPacketSender(instance, "createAssignID", PacketHeader.ASSIGN_ID);
		pm.registerPacketSender(instance, "createDisconnect", PacketHeader.DISCONNECT);
		pm.registerPacketSender(instance, "createUpdateSettings", PacketHeader.UPDATE_SETTINGS);
		pm.registerPacketSender(instance, "createSetReady", PacketHeader.SET_READY);
		pm.registerPacketSender(instance, "createInterruptReady", PacketHeader.INTERRUPT_READY);
		pm.registerPacketSender(instance, "createBeginCountdown", PacketHeader.BEGIN_COUNTDOWN);
		pm.registerPacketSender(instance, "createSyncCountdown", PacketHeader.SYNC_COUNTDOWN);
		pm.registerPacketSender(instance, "createError", PacketHeader.ERROR);
		pm.registerPacketSender(instance, "createChangeName", PacketHeader.CHANGE_NAME);
		pm.registerPacketSender(instance, "createSendNetInfo", PacketHeader.SEND_NET_INFO);

		pm.registerClass(instance);
	}

	@PacketHandler(priority=1, header=PacketHeader.ANY)
	public void packetAnnouncer(Peer peer, ParsedPacket parsed) {
		if(parsed.getHeader() == PacketHeader.PING || parsed.getHeader() == PacketHeader.RETURN_PING)
			return;
		String clName = parsed.getClass().getSimpleName();
		String headerName = parsed.getHeader() != null ? parsed.getHeader().name() : "NULL_HEADER";
		String peerName = peer != null ? peer.getName() : "NULL_PEER";
		System.out.println("Recieved packet " + clName + " (" + headerName + ") from " + peerName);
	}
	
	@PacketHandler(priority=3, header=PacketHeader.ERROR) 
	public void onError(Peer peer, ParsedPacket parsed) {
		System.err.println(((ErrorPacket) parsed).getReason());
	}

	/**
	 * Parses a ping packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the parsed packet
	 */
	public ParsedPacket parsePing(PacketHeader header, ByteBuffer buffer) {
		long timeSent = buffer.getLong();

		return new PingPacket(timeSent);
	}

	/**
	 * Writes a ping packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments Length 1, contains a long (timeSent)
	 */
	public void createPing(ByteBuffer buffer, Object... arguments) {
		long timeSent = (long) arguments[0];

		buffer.putLong(timeSent);
	}
	
	/**
	 * Parses a return ping packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the parsed packet
	 */
	public ParsedPacket parseReturnPing(PacketHeader header, ByteBuffer buffer) {
		long timeSent = buffer.getLong();

		return new ReturnPingPacket(timeSent);
	}

	/**
	 * Writes a return ping packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments Length 1, contains a long (timeSent)
	 */
	public void createReturnPing(ByteBuffer buffer, Object... arguments) {
		long timeSent = (long) arguments[0];

		buffer.putLong(timeSent);
	}

	/**
	 * Parses a connect packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the parsed packet
	 */
	public ParsedPacket parseConnect(PacketHeader header, ByteBuffer buffer) { 
		boolean idAssigned = buffer.get() == 1;
		boolean ready = buffer.get() == 1;
		int id = -1;
		if(idAssigned)
			id = buffer.getInt();
		int nmLen = buffer.getInt();
		String name = NetUtils.readString(buffer, nmLen);

		return new ConnectPacket(name, idAssigned, ready, id);
	}

	/**
	 * Writes a connect packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments either a peer, or (a boolean (if an id has been assigned to this peer yet), another boolean (readyness),
	 * followed by an int IF the first boolean was true (the assigned id), followed by a string (name))
	 */
	public void createConnect(ByteBuffer buffer, Object... arguments) {
		if(arguments[0] instanceof Peer) {
			createConnectByPeer(buffer, (Peer) arguments[0]);
			return;
		}

		boolean assignedId = (boolean) arguments[0];
		boolean ready = (boolean) arguments[1];
		int id = -1;
		String name;
		if(assignedId) {
			id = (int) arguments[2];
			name = (String) arguments[3];
		}else {
			name = (String) arguments[2];
		}

		buffer.put(!assignedId ? (byte) 0 : (byte) 1);
		buffer.put(!ready ? (byte) 0 : (byte) 1);
		if(assignedId)
			buffer.putInt(id);
		buffer.putInt(name.length());
		NetUtils.putString(buffer, name);
	}

	/**
	 * Writes a connect packet based on a peer. This should only be called by createConnect.
	 * Assumes the peer has an id assigned already
	 * 
	 * @param buffer the buffer
	 * @param peer the peer
	 */
	public void createConnectByPeer(ByteBuffer buffer, Peer peer) {
		buffer.put((byte) 1);
		buffer.put(peer.isReady() ? (byte) 1 : (byte) 0);
		buffer.putInt(peer.getID());
		buffer.putInt(peer.getName().length());
		NetUtils.putString(buffer, peer.getName());
	}

	/**
	 * Parses a assignid packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the parsed packet
	 */
	public ParsedPacket parseAssignId(PacketHeader header, ByteBuffer buffer) {
		boolean request = buffer.get() == 1;
		int id = -1;
		int peerID = -1;
		if(!request) {
			id = buffer.getInt();
			peerID = buffer.getInt(); // underflows because it hates america
		}

		return new AssignIDPacket(request, id, peerID);
	}

	/**
	 * Writes an assign id packet to the buffer, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments a boolean (true for requesting to be assigned an id, false to assign an id). If false,
	 * an integer for the id that is being assigned
	 */
	public void createAssignID(ByteBuffer buffer, Object... arguments) {
		boolean request = (boolean) arguments[0];
		int id = -1;
		int relevantPeerID = -1;
		if(!request) {
			id = (int) arguments[1];
			relevantPeerID = (int) arguments[2];
		}

		if(request) {
			buffer.put((byte) 1);
		}else {
			buffer.put((byte) 0);
			buffer.putInt(id);
			buffer.putInt(relevantPeerID);
		}
	}

	/**
	 * Parses a disconnect packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the parsed packet
	 */
	public ParsedPacket parseDisconnect(PacketHeader header, ByteBuffer buffer) {
		int reasonLen = buffer.getInt();
		String reason = NetUtils.readString(buffer, reasonLen);

		return new DisconnectPacket(reason);
	}

	/**
	 * Writes a disconnect packet to the buffer, should not be called outside of the packet manager
	 * @param buffer the buffer to write into
	 * @param arguments a string, for the reason
	 */
	public void createDisconnect(ByteBuffer buffer, Object... arguments) {
		String reason = (String) arguments[0];

		buffer.putInt(reason.length());
		NetUtils.putString(buffer, reason);
	}

	/**
	 * Parses an update settings packet, should not be called outside of the packet manager.
	 * @param header the header
	 * @param buffer the buffer
	 * @return a UpdateSettingsPacket
	 */
	public ParsedPacket parseUpdateSettings(PacketHeader header, ByteBuffer buffer) {
		GameSettingsBuilder builder = new GameSettingsBuilder();

		return new UpdateSettingsPacket(builder.create());
	}

	/**
	 * Writes an update settings packet, should not be called outside of the packet manager
	 * @param buffer the buffer to write into
	 * @param arguments the game settings (1 object)
	 */
	public void createUpdateSettings(ByteBuffer buffer, Object... arguments) {
		//		GameSettings settings = (GameSettings) arguments[0];

		// actually write settings
	}

	/**
	 * Parses a set ready packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer to parse out of
	 * @return a SetReadyPacket
	 */
	public ParsedPacket parseSetReady(PacketHeader header, ByteBuffer buffer) {
		boolean ready = (buffer.get() == 1);

		return new SetReadyPacket(ready);
	}

	/**
	 * Creates a set ready packet, should not be called outside of the packet manager
	 * @param buffer the buffer to write into
	 * @param arguments the arguments, 1 boolean (Boolean) of readyness
	 */
	public void createSetReady(ByteBuffer buffer, Object... arguments) {
		boolean ready = (boolean) arguments[0];

		buffer.put(ready ? (byte) 1 : (byte) 0);
	}

	/**
	 * Parses a interrupt-ready packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer to parse out of
	 * @return a interrupt ready packet
	 */
	public ParsedPacket parseInterruptReady(PacketHeader header, ByteBuffer buffer) {
		return new InterruptReadyPacket();
	}

	/**
	 * Creates an interrupt-ready packet, should not be called outside the packet manager
	 * @param buffer the buffer to write into
	 * @param arguments empty/null
	 */
	public void createInterruptReady(ByteBuffer buffer, Object... arguments) {

	}

	/**
	 * Parses a begin-countdown packet, should not be called outside of the packet manager
	 * @param header the header (BEGIN_COUNTDOWN)
	 * @param buffer the buffer
	 * @return the begin countdown packet
	 */
	public ParsedPacket parseBeginCountdown(PacketHeader header, ByteBuffer buffer) {
		return new BeginCountdownPacket();
	}

	/**
	 * Creates a begin-countdown packet, should not be called outside the packet manager
	 * @param buffer the buffer to write into
	 * @param args empty/null
	 */
	public void createBeginCountdown(ByteBuffer buffer, Object... args) {

	}

	/**
	 * Parses a sync countdown packet, should not be called outside the packet manager
	 * @param header the header 
	 * @param buffer the buffer
	 * @return a sync countdown packet
	 */
	public ParsedPacket parseSyncCountdown(PacketHeader header, ByteBuffer buffer) {
		int newNum = buffer.getInt();

		return new CountdownChangedPacket(newNum);
	}

	/**
	 * Creates a sync countdown packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments an integer of the seconds remaining
	 */
	public void createSyncCountdown(ByteBuffer buffer, Object... arguments) {
		int secsRemaining = (int) arguments[0];

		buffer.putInt(secsRemaining);
	}

	/**
	 * Parses an error packet, should not be called outside the packet manager
	 * @param header the header 
	 * @param buffer the buffer
	 * @return an error packet
	 */
	public ParsedPacket parseError(PacketHeader header, ByteBuffer buffer) {
		int reasonLen = buffer.getInt();
		String reason = NetUtils.readString(buffer, reasonLen);

		return new ErrorPacket(reason);
	}

	/**
	 * Creates an error packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments a string, of the reason an error resulted
	 */
	public void createError(ByteBuffer buffer, Object... arguments) {
		String reason = (String) arguments[0];

		buffer.putInt(reason.length());
		NetUtils.putString(buffer, reason);
	}
	
	/**
	 * Parses a change name packet, should not be called outside the packet manager
	 * @param header the header (change name)
	 * @param buffer the buffer to read from
	 * @return the change name packet
	 */
	public ParsedPacket parseChangeName(PacketHeader header, ByteBuffer buffer) {
		int newNameLen = buffer.getInt();
		String newName = NetUtils.readString(buffer, newNameLen);
		
		return new ChangeNamePacket(newName);
	}
	
	/**
	 * Creates a change name packet, should not be called outside of the packet manager
	 * @param buffer the buffer
	 * @param arguments a string, the new name you want
	 */
	public void createChangeName(ByteBuffer buffer, Object... arguments) {
		String newName = (String) arguments[0];
		buffer.putInt(newName.length());
		NetUtils.putString(buffer, newName);
	}
	
	/**
	 * Parses a send net info packet, should not be called outside of the packet manager
	 * @param header the header
	 * @param buffer the buffer
	 * @return the send net info packet
	 */
	public ParsedPacket parseSendNetInfo(PacketHeader header, ByteBuffer buffer) {
		int nClients = buffer.getInt();
		Map<Integer, InetSocketAddress> addresses = new HashMap<>();
		
		for(int i = 0; i < nClients; i++) {
			int id = buffer.getInt();
			byte size = buffer.get(); // 4 or 6
			if(size != 4 && size != 6) {
				System.err.println("Unexpected size retrieved in send net info: " + size);
				size = 6; // since we're allocating an array, we should limit it a tad
			}
			byte[] arr = new byte[size];
			for(int j = 0; j < size; j++) {
				arr[j] = buffer.get();
			}
			InetSocketAddress sAddr = null;
			
			try {
				sAddr = new InetSocketAddress(InetAddress.getByAddress(arr).getHostAddress(), NetUtils.PORT + (id - NetUtils.INIT_ID));
			} catch (UnknownHostException e) {
				System.out.println("Attempted to parse a buffer with an invalid inet address " + Arrays.toString(arr));
				e.printStackTrace();
			}
			addresses.put(id, sAddr);
		}
		return new SendNetInfoPacket(addresses);
	}
	
	/**
	 * Creates a send net info packet. Should not be called outside of the packet manager
	 * Arguments:
	 * <pre>
	 * [
	 *   0: Number of clients 
	 *   1: id of client 1 
	 *   2: address of client 1 
	 *   3: id of client 2 
	 *   4: address of client 2
	 *   .....
	 *   n*2 - 1: id of client n  where n is 1-based
	 *   n*2: address of client n 
	 * ]</pre>
	 * 
	 * Address should be an InetSocketAddress
	 * @param buffer the buffer
	 * @param args see method doc
	 */
	public void createSendNetInfo(ByteBuffer buffer, Object... args) {
		int numClients = (int) args[0];
		buffer.putInt(numClients);
		
		for(int i = 0; i < numClients; i++) {
			int id = (int) args[i * 2 + 1];
			InetSocketAddress sAddr = (InetSocketAddress) args[i * 2 + 2];
			InetAddress addr = sAddr.getAddress();
			byte[] addrArr = addr.getAddress();
			
			buffer.putInt(id);
			buffer.put((byte) addrArr.length);
			
			for(int j = 0; j < addrArr.length; j++) { buffer.put(addrArr[j]); }
		}
	}
}
