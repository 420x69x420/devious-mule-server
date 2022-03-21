package org.lostclient.muling.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.lostclient.muling.Client;
import org.lostclient.muling.Log;
import org.lostclient.muling.Request;
import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;
import org.lostclient.muling.messages.MuleTile;
import org.lostclient.muling.messages.client.*;
import org.lostclient.muling.messages.server.MuleResponseMessage;
import org.lostclient.muling.messages.server.TradeResponseMessage;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server extends WebSocketServer
{
	private long connIndex = 0L;
	private final Map<Long, Client> clients = new ConcurrentHashMap<>();
	private final List<Request> requests = new CopyOnWriteArrayList<>();

	public Server(int port)
	{
		super(new InetSocketAddress(port));
	}

	private Client getClientFromConn(WebSocket conn, ClientHandshake handshake)
	{
		if (handshake.getFieldValue("clientUsername").length() == 0
				|| handshake.getFieldValue("playerName").length() == 0
				|| handshake.getFieldValue("isMule").length() == 0
				|| handshake.getFieldValue("isMember").length() == 0)
		{
			conn.close(10001, "Invalid or missing handshake data");
			return null;
		}

		String clientUsername = handshake.getFieldValue("clientUsername");
		String group = handshake.getFieldValue("group").length() > 0 ? handshake.getFieldValue("group") : "default";
		String playerName = handshake.getFieldValue("playerName");
		boolean isMule = handshake.getFieldValue("isMule").equals("true");
		boolean isMember = handshake.getFieldValue("isMember").equals("true");
		int muleWorldId = 0;
		MuleTile muleTile = null;

		if (isMule)
		{
			if (handshake.getFieldValue("worldId").length() == 0
					|| handshake.getFieldValue("tileX").length() == 0
					|| handshake.getFieldValue("tileY").length() == 0
					|| handshake.getFieldValue("tileZ").length() == 0)
			{
				conn.close(10002, "Invalid or missing mule handshake data");
				return null;
			}
			muleWorldId = Integer.parseInt(handshake.getFieldValue("worldId"));
			muleTile = new MuleTile(
					Integer.parseInt(handshake.getFieldValue("tileX")),
					Integer.parseInt(handshake.getFieldValue("tileY")),
					Integer.parseInt(handshake.getFieldValue("tileZ"))
			);
		}

		conn.setAttachment(connIndex);

		Client client = new Client(conn, connIndex, System.currentTimeMillis(), clientUsername, group, playerName, isMule, isMember);

		client.setWorldId(muleWorldId);
		client.setTile(muleTile);

		clients.put(connIndex, client);

		connIndex++;

		return client;
	}

	private Client getClientFromConn(WebSocket conn)
	{
		long connIndex = conn.<Long>getAttachment();
		return clients.getOrDefault(connIndex, null);
	}

	private void removeClient(Client client)
	{
		clients.remove(client.getConnIndex());
	}

	@Override
	public void onStart()
	{
		Log.info("LostMuleServer started!");
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		Client client = getClientFromConn(conn, handshake);
		if (client == null)
		{
			return;
		}
		Log.info("A client connected: " + client);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		Client client = getClientFromConn(conn);
		if (client == null)
		{
			return;
		}
		Log.info("A client disconnected: " + client + " - code: " + code + " - reason: " + reason);
		removeClient(client);
	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		Client client = getClientFromConn(conn);
		if (client == null)
		{
			return;
		}
		Log.info("A client caused an error: " + client + " - " + ex);
	}

	@Override
	public void onMessage(WebSocket conn, String message)
	{
		Client client = getClientFromConn(conn);
		if (client == null)
		{
			return;
		}

		Log.info("Client message: " + conn.getRemoteSocketAddress() + " - " + message);

		JsonElement jsonElement = new JsonParser().parse(message);
		if (jsonElement == null)
		{
			return;
		}

		JsonObject jsonObject = jsonElement.getAsJsonObject();
		if (!jsonObject.has("type"))
		{
			return;
		}

		try
		{
			MessageType messageType = MessageType.valueOf(jsonObject.get("type").getAsString());

			switch (messageType)
			{
				case OWNED_ITEMS_UPDATE:
				{
					OwnedItemsUpdateMessage ownedItemsUpdate = new Gson().fromJson(jsonElement, OwnedItemsUpdateMessage.class);
					client.getOwnedItems().clear();
					client.getOwnedItems().addAll(ownedItemsUpdate.ownedItems);
				}
				break;

				case MULE_REQUEST:
				{
					MuleRequestMessage muleRequest = new Gson().fromJson(jsonElement, MuleRequestMessage.class);
					requests.removeIf(request -> request.getMuleRequest().playerName.equals(muleRequest.playerName));

					Client mule = findMuleForRequest(client.getGroup(), muleRequest);
					if (mule == null)
					{
						send(conn, new MuleResponseMessage(false, muleRequest.requestId, 0, null));
						return;
					}
					requests.add(new Request(client, mule, muleRequest));
					send(conn, new MuleResponseMessage(true, muleRequest.requestId, mule.getWorldId(), mule.getTile()));
					send(mule.getConn(), muleRequest);
				}
				break;

				case TRADE_REQUEST:
				{
					TradeRequestMessage tradeRequest = new Gson().fromJson(jsonElement, TradeRequestMessage.class);
					for (Request request : requests)
					{
						if (request.getMuleRequest().requestId.equals(tradeRequest.requestId))
						{
							send(conn, new TradeResponseMessage(true, tradeRequest.requestId, request.getMule().getPlayerName()));
							break;
						}
					}
				}
				break;

				case TRADE_COMPLETED:
				{
					TradeCompletedMessage tradeCompleted = new Gson().fromJson(jsonElement, TradeCompletedMessage.class);
					for (Request request : requests)
					{
						if (request.getMuleRequest().requestId.equals(tradeCompleted.requestId))
						{
							if (client.isMule())
							{
								send(request.getClient().getConn(), tradeCompleted);
							}
							requests.remove(request);
						}
					}
				}
				break;

				case UNKNOWN_TRADER:
				{
					UnknownTraderMessage unknownTrader = new Gson().fromJson(jsonElement, UnknownTraderMessage.class);
					for (Request request : requests)
					{
						if (request.getMuleRequest().playerName.equals(unknownTrader.playerName))
						{
							send(request.getClient().getConn(), unknownTrader);
						}
					}
				}
				break;
			}
		}
		catch (IllegalArgumentException ex)
		{
			ex.printStackTrace();
		}
	}

	public void send(WebSocket conn, AbstractMessage message)
	{
		if (conn.isClosed())
		{
			return;
		}
		String data = message.toJson().toString();
		Log.info("Sending message to conn: " + conn.getRemoteSocketAddress() + " - " + data);
		conn.send(data);
	}

	private Client findMuleForRequest(String group, MuleRequestMessage request)
	{
		for (Client client : clients.values())
		{
			if (!client.isMule())
			{
				continue;
			}

			if (!client.getGroup().equals(group))
			{
				continue;
			}

			if (request.requiredItems.size() > 0 && !client.hasRequiredItems(request.requiredItems))
			{
				continue;
			}

			return client;
		}
		return null;
	}
}
