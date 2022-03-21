package org.lostclient.muling.messages.client;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;

public class UnknownTraderMessage extends AbstractMessage
{
	public final String playerName;

	public UnknownTraderMessage(String playerName)
	{
		super(MessageType.UNKNOWN_TRADER);
		this.playerName = playerName;
	}
}
