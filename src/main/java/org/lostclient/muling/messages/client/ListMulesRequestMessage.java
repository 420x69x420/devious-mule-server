package org.lostclient.muling.messages.client;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;

public class ListMulesRequestMessage extends AbstractMessage
{
	public ListMulesRequestMessage()
	{
		super(MessageType.LIST_MULES_REQUEST);
	}
}
