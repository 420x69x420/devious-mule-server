package org.lostclient.muling.messages.client;

import org.lostclient.muling.OfferedItem;
import org.lostclient.muling.RequiredItem;
import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;

import java.util.List;

public class MuleRequestMessage extends AbstractMessage
{
	public final String requestId;
	public final long requestedAt;
	public final String playerName;
	public final boolean hasMembership;
	public final List<RequiredItem> requiredItems;
	public final List<OfferedItem> offeredItems;

	public MuleRequestMessage(String requestId, long requestedAt, String playerName, boolean hasMembership, List<RequiredItem> requiredItems, List<OfferedItem> offeredItems)
	{
		super(MessageType.MULE_REQUEST);
		this.requestId = requestId;
		this.requestedAt = requestedAt;
		this.playerName = playerName;
		this.hasMembership = hasMembership;
		this.requiredItems = requiredItems;
		this.offeredItems = offeredItems;
	}
}
