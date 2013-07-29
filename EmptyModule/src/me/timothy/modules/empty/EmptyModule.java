package me.timothy.modules.empty;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import me.timothy.dcrts.net.LogicModule;
import me.timothy.dcrts.net.module.ModuleHandler;
import me.timothy.dcrts.net.packets.ChangeModulePacket;
import me.timothy.dcrts.packet.PacketHandler;
import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.PacketListener;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.peer.Peer;
import me.timothy.dcrts.utils.ErrorUtils;
import me.timothy.dcrts.utils.NetUtils;

public class EmptyModule extends LogicModule implements PacketListener {

	@Override
	public void onActivate() {
		pManager.registerClass(this);
		System.out.println("EmptyModule activated!");
	}

	@Override
	public void onDeactivate() {
		pManager.unregisterClass(this);
		System.out.println("EmptyModule deactivated!");
	}
	
	@PacketHandler(header=PacketHeader.CHANGE_MODULE, priority=3)
	public void onChangeModulePacket(Peer peer, ParsedPacket packet) {
		ChangeModulePacket cmp = (ChangeModulePacket) packet;
		System.out.println("Recieved change module packet in EmptyModule: id=" + cmp.getPeer());
		
		int peerId = cmp.getPeer();
		Peer affectedPeer = cmp.getPeer() == gameState.getLocalPeer().getID() ?
										 gameState.getLocalPeer() :
										 NetUtils.getPeerByID(gameState.getConnectedPeers(), peerId);
		
		File moduleLocation = ModuleHandler.getFileForModule(cmp.getModule());
		byte[] sha1Hash;
		try {
			sha1Hash = NetUtils.sha1Hash(moduleLocation);
		} catch (IOException e) {
			System.err.println("Failed to parse sha1 hash of " + moduleLocation);
			e.printStackTrace();
			return;
		}
		
		if(!Arrays.equals(sha1Hash, cmp.getExpectedSHA1Hash())) {
			System.err.println("SHA1 Hash of the requested module does NOT match the SHA1 hash of the module to change to!");
			System.err.println("To prevent a security breach, shutting down");
			// TODO instead of shutting down, display a message & sync
			System.exit(1);
		}
		if(affectedPeer == null || cmp == null)
			ErrorUtils.nullPointer(new String[] { "affectedPeer", "cmp" }, affectedPeer, cmp);
		System.out.println("Changing 1 module of " + affectedPeer.getName() + " (" + affectedPeer.getID() + ") to " + cmp.getModule());
		if(cmp.isNetModule()) {
			netState.setNetModuleOf(affectedPeer, ModuleHandler.getNetModuleByName(cmp.getModule()));
		}else {
			netState.setLogicTypeOf(affectedPeer, ModuleHandler.getLogicModuleByName(cmp.getModule()));
		}
	}
}
