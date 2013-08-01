package me.timothy.dcrts.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.peer.Peer;

public class NetUtils {
	public static final int RESERVED_ID = Integer.MAX_VALUE;
	public static final int INIT_ID = 1337;
	public static final int PORT = 25994;
	private static final int DIRECT_PORT = PORT - 1;

	/**
	 * Reads a string from the specified buffer of lengthCh length
	 * @param buffer the buffer to read from
	 * @param lengthCh the length (in characters)
	 * @return the string
	 */
	public static String readString(ByteBuffer buffer, int lengthCh) {
		StringBuilder res = new StringBuilder();
		for(int i = 0; i < lengthCh; i++) {
			res.append(buffer.getChar());
		}
		return res.toString();
	}

	public static void putString(ByteBuffer buffer, String string) {
		for(int i = 0; i < string.length(); i++) {
			buffer.putChar(string.charAt(i));
		}
	}

	public static Peer getPeerByID(List<Peer> arr, int peerId) {
		synchronized(arr) {
			for(Peer peer : arr) {
				if(peer.getID() == peerId)
					return peer;
			}
		}
		return null;
	}

	public static ByteBuffer createBuffer(int id, PacketHeader header) {
		ByteBuffer buffer = ByteBuffer.allocate(header.getMaxPacketSize());
		buffer.putInt(id);
		buffer.putInt(header.getValue());
		return buffer;
	}

	public static byte[] sha1Hash(File fileForModule) throws IOException {
	    MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MessageDigest does not know of SHA1!");
		}
	    FileInputStream fis = new FileInputStream(fileForModule);
	    byte[] dataBytes = new byte[1024];
	 
	    int nread = 0; 
	 
	    while ((nread = fis.read(dataBytes)) != -1) {
	      md.update(dataBytes, 0, nread);
	    };
	 
	    fis.close();
		return md.digest();
	}
	
	public static boolean addressMatches(InetSocketAddress addr1, InetSocketAddress addr2) {
		String hName1 = addr1.getHostName();
		String hName2 = addr2.getHostName();
		if(hName1.equals("127.0.0.1"))
			hName1 = "localhost";
		if(hName2.equals("127.0.0.1"))
			hName2 = "localhost";
		
		return hName1.equals(hName2);
	}
	
	public static int getDirectPort(int peerId) {
		return (DIRECT_PORT - 1) - (peerId - INIT_ID);
	}

	public static ByteBuffer createBufferNoID(PacketHeader header) {
		ByteBuffer buffer = ByteBuffer.allocate(header.getMaxPacketSize());
		buffer.putInt(header.getValue());
		
		return buffer;
	}
}
