package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;

public class ChangeModulePacket implements ParsedPacket {
	private int peer;
	private String moduleName;
	private byte[] sha1Hash;
	private boolean netModule;
	
	public ChangeModulePacket(int p, String mod, boolean net, byte[] hash) {
		peer = p;
		moduleName = mod;
		sha1Hash = hash;
		netModule = net;
	}
	
	/**
	 * @return the id of the peer
	 */
	public int getPeer() {
		return peer;
	}
	
	public String getModule() {
		return moduleName;
	}
	
	/**
	 * False means logic module
	 * @return if this is a net module
	 */
	public boolean isNetModule() {
		return netModule;
	}
	
	public byte[] getExpectedSHA1Hash() {
		return sha1Hash;
	}
	
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.CHANGE_MODULE;
	}

}
