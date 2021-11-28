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
import org.lostclient.muling.messages.client.RegisterRequestMessage;
import org.lostclient.muling.messages.client.TradeRequestMessage;
import org.lostclient.muling.messages.server.MuleResponseMessage;
import org.lostclient.muling.messages.server.RegisterResponseMessage;
import org.lostclient.muling.messages.server.TradeResponseMessage;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Server extends WebSocketServer
{
	private final ReentrantLock connIndexMutex = new ReentrantLock();
	private long connIndex = 0L;
	private final Map<Long, Client> clients = new ConcurrentHashMap<>();
	private final List<Request> requests = new CopyOnWriteArrayList<>();

	public Server(int port)
	{
		super(new InetSocketAddress(port));
	}

	@Override
	public void onStart()
	{
		Log.info("LostMuleServer started!");
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		connIndexMutex.lock();
		conn.setAttachment(connIndex);
		Client client = new Client(conn, connIndex, System.currentTimeMillis());
		Log.info("A new client has connected: " + conn.getRemoteSocketAddress() + " - connIndex: " + connIndex);
		clients.put(connIndex, client);
		connIndex++;
		connIndexMutex.unlock();
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		Client client = getClientFromConn(conn);
		if (client == null)
		{
			Log.severe("An unknown client disconnected from the server: " + conn.getRemoteSocketAddress() + " - " + code + " - " + reason);
			return;
		}
		Log.info("A client has disconnected from the server: " + conn.getRemoteSocketAddress() + " - " + code + " - " + reason);
		clients.remove(client.getConnIndex());
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
				case REGISTER_REQUEST:
					RegisterRequestMessage registerRequest = new Gson().fromJson(jsonElement, RegisterRequestMessage.class);
					client.setMule(true);
					client.setWorldId(registerRequest.worldId);
					client.setTile(registerRequest.tile);
					client.setPlayerName(registerRequest.playerName);
					client.setHasMembership(registerRequest.hasMembership);
					send(conn, new RegisterResponseMessage(true));
					break;

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
		if (conn == null)
		{
			Log.severe("Unknown client connection error: " + ex + " - " + ex.getMessage());
			return;
		}

		Client client = getClientFromConn(conn);
		if (client == null)
		{
			Log.severe("Unknown client connection error: " + conn.getRemoteSocketAddress() + " - " + ex + " - " + ex.getMessage());
			return;
		}

		Log.info("A client caused an error: " + conn.getRemoteSocketAddress() + " - " + ex + " - " + ex.getMessage());
	}

	private Client getClientFromConn(WebSocket conn)
	{
		long connIndex = conn.<Long>getAttachment();
		return clients.getOrDefault(connIndex, null);
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
