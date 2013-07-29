package me.timothy.dcrts.net.packets;

import me.timothy.dcrts.packet.PacketHeader;
import me.timothy.dcrts.packet.ParsedPacket;
import me.timothy.dcrts.settings.GameSettings;

public class UpdateSettingsPacket implements ParsedPacket {
	private GameSettings settings;
	
	public UpdateSettingsPacket(GameSettings settings) {
		this.settings = settings;
	}
	
	public GameSettings getSettings() {
		return settings;
	}
	
	@Override
	public PacketHeader getHeader() {
		return PacketHeader.UPDATE_SETTINGS;
	}

}
