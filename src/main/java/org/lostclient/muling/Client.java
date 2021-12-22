package org.lostclient.muling;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.java_websocket.WebSocket;
import org.lostclient.muling.messages.MuleTile;
import org.lostclient.muling.messages.OwnedItem;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class Client
{
	private final WebSocket conn;
	private final long connIndex;
	private final long connectedAt;
	private final String address;
	private final String clientUsername;
	private final String playerName;
	private final boolean isMule;
	private final boolean isMember;
	private int worldId;
	private MuleTile tile;
	private final List<OwnedItem> ownedItems = new ArrayList<>();

	public Client(WebSocket conn, long connIndex, long connectedAt, String clientUsername, String playerName, boolean isMule, boolean isMember)
	{
		this.conn = conn;
		this.connIndex = connIndex;
		this.connectedAt = connectedAt;
		this.address = conn.getRemoteSocketAddress().toString();
		this.clientUsername = clientUsername;
		this.playerName = playerName;
		this.isMule = isMule;
		this.isMember = isMember;
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
