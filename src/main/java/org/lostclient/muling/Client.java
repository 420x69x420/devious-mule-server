package org.lostclient.muling;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.java_websocket.WebSocket;
import org.lostclient.muling.messages.MuleTile;
import org.lostclient.muling.messages.OwnedItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final String group;
	private final String playerName;
	private final boolean isMule;
	private final boolean isMember;
	private int worldId;
	private MuleTile tile;
	private final List<OwnedItem> ownedItems = new ArrayList<>();

	public Client(WebSocket conn, long connIndex, long connectedAt, String clientUsername, String group, String playerName, boolean isMule, boolean isMember)
	{
		this.conn = conn;
		this.connIndex = connIndex;
		this.connectedAt = connectedAt;
		this.address = conn.getRemoteSocketAddress().toString();
		this.clientUsername = clientUsername;
		this.group = group;
		this.playerName = playerName;
		this.isMule = isMule;
		this.isMember = isMember;
	}

	public boolean hasRequiredItems(List<RequiredItem> requiredItems, List<Request> requests)
	{
		Map<Integer, Integer> requestCounts = new HashMap<>();
		for (Request request : requests)
		{
			request.getMuleRequest().requiredItems.forEach(i ->
					requestCounts.put(i.getItemId(), i.getQuantity() + requestCounts.getOrDefault(i.getItemId(), 0)));
		}

		for (RequiredItem requiredItem : requiredItems)
		{
			if (getOwnedItems().stream().noneMatch(ownedItem ->
			{
				int ownedQuantity = ownedItem.getQuantity() - requestCounts.getOrDefault(ownedItem.getItemId(), 0);
				return ownedItem.getItemId() == requiredItem.getItemId() && ownedQuantity >= requiredItem.getQuantity();
			}))
			{
				return false;
			}
		}

		return true;
	}

	public List<OwnedItem> getRemainingItems(List<Request> requests)
	{
		Map<Integer, Integer> requestCounts = new HashMap<>();
		for (Request request : requests)
		{
			request.getMuleRequest().requiredItems.forEach(i ->
					requestCounts.put(i.getItemId(), i.getQuantity() + requestCounts.getOrDefault(i.getItemId(), 0)));
		}

		List<OwnedItem> remainingItems = new ArrayList<>();

		for (OwnedItem ownedItem : getOwnedItems())
		{
			int ownedQuantity = ownedItem.getQuantity() - requestCounts.getOrDefault(ownedItem.getItemId(), 0);
			remainingItems.add(new OwnedItem(
					ownedItem.getItemId(),
					ownedQuantity
			));
		}

		return remainingItems;
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof Client && ((Client) other).connIndex == connIndex;
	}
}
