package org.lostclient.muling.messages.client;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;

public class TradeCompletedMessage extends AbstractMessage
{
	public final boolean success;
	public final String requestId;

	public TradeCompletedMessage(boolean success, String requestId)
	{
		super(MessageType.TRADE_COMPLETED);
		this.success = success;
		this.requestId = requestId;
	}
}
