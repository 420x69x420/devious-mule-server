package org.lostclient.muling;

import lombok.Getter;
import lombok.Setter;
import org.java_websocket.WebSocket;
import org.lostclient.muling.messages.MuleTile;
import org.lostclient.muling.messages.OwnedItem;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Client
{
	private final WebSocket conn;
	private final long connIndex;
	private final long connectedAt;
	private final String address;
	private boolean isMule;
	private int worldId;
	private MuleTile tile;
	private String playerName;
	private boolean hasMembership;
	private final List<OwnedItem> ownedItems = new ArrayList<>();

	public Client(WebSocket conn, long connIndex, long connectedAt)
	{
		this.conn = conn;
		this.connIndex = connIndex;
		this.connectedAt = connectedAt;
		this.address = conn.getRemoteSocketAddress().toString();
	}

	public boolean hasRequiredItems(List<RequiredItem> requiredItems)
	{
		for (RequiredItem requiredItem : requiredItems)
		{
			if (ownedItems.stream().noneMatch(ownedItem -> ownedItem.getItemId() == requiredItem.getItemId() && ownedItem.getQuantity() >= requiredItem.getQuantity()))
			{
				return false;
			}
		}
		return true;
	}
}
