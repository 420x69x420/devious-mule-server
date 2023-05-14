package org.lostclient.muling.messages.server;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;

public class TradeResponseMessage extends AbstractMessage
{
	public final boolean success;
	public final String errorMessage;
	public final String requestId;
	public final String playerName;

	public TradeResponseMessage(boolean success, String errorMessage, String requestId, String playerName)
	{
		super(MessageType.TRADE_RESPONSE);
		this.success = success;
		this.errorMessage = errorMessage;
		this.requestId = requestId;
		this.playerName = playerName;
	}
}
