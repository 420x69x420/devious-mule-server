package org.lostclient.muling.messages.client;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;
import org.lostclient.muling.messages.OwnedItem;

import java.util.List;

public class OwnedItemsUpdateMessage extends AbstractMessage
{
	public final List<OwnedItem> ownedItems;

	public OwnedItemsUpdateMessage(List<OwnedItem> ownedItems)
	{
		super(MessageType.OWNED_ITEMS_UPDATE);
		this.ownedItems = ownedItems;
	}
}
