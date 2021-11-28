package org.lostclient.muling.messages.client;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;
import org.lostclient.muling.messages.MuleTile;

public class RegisterRequestMessage extends AbstractMessage
{
	public final int worldId;
	public final MuleTile tile;
	public final String playerName;
	public final boolean hasMembership;

	public RegisterRequestMessage(int worldId, MuleTile tile, String playerName, boolean hasMembership)
	{
		super(MessageType.REGISTER_REQUEST);
		this.worldId = worldId;
		this.tile = tile;
		this.playerName = playerName;
		this.hasMembership = hasMembership;
	}
}
