package org.lostclient.muling.messages.client;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;

public class TradeRequestMessage extends AbstractMessage
{
	public final String requestId;

	public TradeRequestMessage(String requestId)
	{
		super(MessageType.TRADE_REQUEST);
		this.requestId = requestId;
	}
}
