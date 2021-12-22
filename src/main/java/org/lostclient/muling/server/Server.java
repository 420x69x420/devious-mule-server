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
import org.lostclient.muling.messages.client.MuleRequestMessage;
import org.lostclient.muling.messages.client.OwnedItemsUpdateMessage;
import org.lostclient.muling.messages.client.TradeRequestMessage;
import org.lostclient.muling.messages.server.MuleResponseMessage;
import org.lostclient.muling.messages.server.TradeResponseMessage;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends WebSocketServer
{
	private long connIndex = 0L;
	private final Map<Long, Client> clients = new HashMap<>();
	private final List<Request> requests = new ArrayList<>();

	public Server(int port)
	{
		super(new InetSocketAddress(port));
	}

	private synchronized Client getClientFromConn(WebSocket conn, ClientHandshake handshake)
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
		String playerName = handshake.getFieldValue("playerName");
		boolean isMule = handshake.getFieldValue("isMule").equals("true");
		boolean isMember = handshake.getFieldValue("isMember").equals("true");

		conn.setAttachment(connIndex);

		Client client = new Client(conn, connIndex, System.currentTimeMillis(), clientUsername, playerName, isMule, isMember);

		clients.put(connIndex, client);

		connIndex++;

		return client;
	}

	private synchronized Client getClientFromConn(WebSocket conn)
	{
		long connIndex = conn.<Long>getAttachment();
		return clients.getOrDefault(connIndex, null);
	}

	private synchronized void removeClient(Client client)
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
		if (client != null)
		{
			Log.info("A client connected: " + client);
		}
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		Client client = getClientFromConn(conn);
		if (client != null)
		{
			Log.info("A client disconnected: " + client + " - code: " + code + " - reason: " + reason);
			removeClient(client);
		}
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
//				case REGISTER_REQUEST:
//					RegisterRequestMessage registerRequest = new Gson().fromJson(jsonElement, RegisterRequestMessage.class);
//					client.setMule(true);
//					client.setWorldId(registerRequest.worldId);
//					client.setTile(registerRequest.tile);
//					client.setPlayerName(registerRequest.playerName);
//					client.setMember(registerRequest.hasMembership);
//					send(conn, new RegisterResponseMessage(true));
//					break;

				case OWNED_ITEMS_UPDATE:
					OwnedItemsUpdateMessage ownedItemsUpdate = new Gson().fromJson(jsonElement, OwnedItemsUpdateMessage.class);
					client.getOwnedItems().clear();
					client.getOwnedItems().addAll(ownedItemsUpdate.ownedItems);
					break;

				case MULE_REQUEST:
					MuleRequestMessage muleRequest = new Gson().fromJson(jsonElement, MuleRequestMessage.class);
					Client mule = findMuleForRequest(muleRequest);
					if (mule == null)
					{
						send(conn, new MuleResponseMessage(false, muleRequest.requestId, 0, null));
						return;
					}
					requests.add(new Request(client, mule, muleRequest));
					send(conn, new MuleResponseMessage(true, muleRequest.requestId, mule.getWorldId(), mule.getTile()));
					send(mule.getConn(), muleRequest);
					break;

				case TRADE_REQUEST:
					TradeRequestMessage tradeRequest = new Gson().fromJson(jsonElement, TradeRequestMessage.class);
					for (Request request : requests)
					{
						if (request.getMuleRequest().requestId.equals(tradeRequest.requestId))
						{
							send(conn, new TradeResponseMessage(true, tradeRequest.requestId, request.getMule().getPlayerName()));
							break;
						}
					}
					break;
			}
		}
		catch (IllegalArgumentException ex)
		{
			System.out.println("Invalid message received");
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		Client client = getClientFromConn(conn);
		if (client != null)
		{
			Log.info("A client caused an error: " + client + " - " + ex);
		}
	}

	public void send(WebSocket conn, AbstractMessage message)
	{
		String data = message.toJson().toString();
		if (conn.isClosed())
		{
			return;
		}
		Log.info("Sending message to conn: " + conn.getRemoteSocketAddress() + " - " + data);
		conn.send(data);
	}

	private Client findMuleForRequest(MuleRequestMessage request)
	{
		for (Client client : clients.values())
		{
			if (!client.isMule() || (request.requiredItems.size() > 0 && !client.hasRequiredItems(request.requiredItems)))
			{
				continue;
			}
			return client;
		}
		return null;
	}
}
