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
import org.lostclient.muling.Random;
import org.lostclient.muling.Request;
import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;
import org.lostclient.muling.messages.Mule;
import org.lostclient.muling.messages.MuleTile;
import org.lostclient.muling.messages.client.MuleRequestMessage;
import org.lostclient.muling.messages.client.OwnedItemsUpdateMessage;
import org.lostclient.muling.messages.client.TradeCompletedMessage;
import org.lostclient.muling.messages.client.TradeRequestMessage;
import org.lostclient.muling.messages.client.UnknownTraderMessage;
import org.lostclient.muling.messages.server.ListMulesResponseMessage;
import org.lostclient.muling.messages.server.MuleResponseMessage;
import org.lostclient.muling.messages.server.TradeResponseMessage;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
		if (conn == null || conn.getAttachment() == null)
		{
			return null;
		}
		long connIndex = conn.<Long>getAttachment();
		return clients.getOrDefault(connIndex, null);
	}

	private void removeClient(Client client, String reason)
	{
		Log.severe(String.format("Removing client: %s - %s", client, reason));

//		if (client.getConn().isOpen())
//		{
//			send(client.getConn(), new TradeCompletedMessage(false, reason, null));
//		}

		clients.remove(client.getConnIndex());
	}

	@Override
	public void onStart()
	{
		Log.info("LostMuleServer started on port: " + getPort());
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
		removeClient(client, String.format("Client disconnected: %s - %d - %s", client, code, reason));
	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		Client client = getClientFromConn(conn);
		if (client == null)
		{
			return;
		}
		removeClient(client, String.format("Client disconnected: %s - %s", client, ex));
	}

	@Override
	public void onMessage(WebSocket conn, String message)
	{
		try
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

			MessageType messageType = MessageType.valueOf(jsonObject.get("type").getAsString());

			switch (messageType)
			{
				case MULE_REQUEST:
				{
					MuleRequestMessage muleRequest = new Gson().fromJson(jsonElement, MuleRequestMessage.class);
					requests.removeIf(request -> request.getMuleRequest().playerName.equals(muleRequest.playerName));

					Client mule = findMuleForRequest(client.getGroup(), muleRequest);
					if (mule == null)
					{
						send(conn, new MuleResponseMessage(false, "Failed to find a mule to handle the request", 0, null));
						return;
					}

					requests.add(new Request(client, mule, muleRequest));

					send(conn, new MuleResponseMessage(true, null, mule.getWorldId(), mule.getTile()));
					send(mule.getConn(), muleRequest);
				}
				break;

				case TRADE_REQUEST:
				{
					TradeRequestMessage tradeRequest = new Gson().fromJson(jsonElement, TradeRequestMessage.class);

					Request matchingRequest = requests.stream()
							.filter(r -> r.getMuleRequest().requestId.equals(tradeRequest.requestId) && r.getMule() != null)
							.findFirst()
							.orElse(null);

					if (matchingRequest == null)
					{
						send(conn, new TradeResponseMessage(false, "Failed to find matching request with id", tradeRequest.requestId,  null));
						return;
					}

					send(conn, new TradeResponseMessage(true, null, tradeRequest.requestId, matchingRequest.getMule().getPlayerName()));
				}
				break;

				// sent from both client and mule for when a trade is completed, successful or not
				case TRADE_COMPLETED:
				{
					TradeCompletedMessage tradeCompleted = new Gson().fromJson(jsonElement, TradeCompletedMessage.class);

					List<Request> matchingRequests = requests
							.stream()
							.filter(r -> r.getMuleRequest().requestId.equals(tradeCompleted.requestId))
							.collect(Collectors.toList());

					for (Request request : matchingRequests)
					{
						// if the sender is a mule, forward the same message onto the bots
						if (client.isMule())
						{
							send(request.getClient().getConn(), tradeCompleted);
						}

						requests.remove(request);
					}
				}
				break;

				// sent from mule when traded from an unknown player, message contains trading player name
				case UNKNOWN_TRADER:
				{
					UnknownTraderMessage unknownTrader = new Gson().fromJson(jsonElement, UnknownTraderMessage.class);

					List<Request> matchingRequests = requests
							.stream()
							.filter(r -> r.getMuleRequest().playerName.equals(unknownTrader.playerName))
							.collect(Collectors.toList());

					for (Request request : matchingRequests)
					{
						send(request.getClient().getConn(), unknownTrader);

						requests.remove(request);
					}
				}
				break;

				// sent from mule whenever inventory changes happen or trades are completed
				case OWNED_ITEMS_UPDATE:
				{
					OwnedItemsUpdateMessage ownedItemsUpdate = new Gson().fromJson(jsonElement, OwnedItemsUpdateMessage.class);

					client.getOwnedItems().clear();
					client.getOwnedItems().addAll(ownedItemsUpdate.ownedItems);
				}
				break;

				// sent from any client to fetch a list of mules connected to server & all their info
				case LIST_MULES_REQUEST:
				{
//					ListMulesRequestMessage listMulesRequestMessage = new Gson().fromJson(jsonElement, ListMulesRequestMessage.class);

					List<Mule> mules = new ArrayList<>();

					for (Client muleClient : clients.values())
					{
						if (!muleClient.isMule())
						{
							continue;
						}

						List<Request> muleRequests = requests.stream()
								.filter(r -> r.getMule() == muleClient)
								.collect(Collectors.toList());

						mules.add(new Mule(
								muleClient.getPlayerName(),
								muleClient.getGroup(),
								muleClient.getWorldId(),
								muleClient.getTile(),
								muleClient.isMember(),
								muleClient.getOwnedItems(),
								muleClient.getRemainingItems(muleRequests),
								muleRequests.size()
						));
					}

					send(conn, new ListMulesResponseMessage(true, null, mules));
				}
				break;
			}
		}
		catch (Exception ex)
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
		List<Client> validMules = new ArrayList<>();
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

			if (request.requiredItems.size() > 0 && !client.hasRequiredItems(request.requiredItems, requests.stream()
					.filter(r -> r.getMule() == client)
					.collect(Collectors.toList())))
			{
				continue;
			}

			validMules.add(client);
		}
		if (validMules.size() == 0)
		{
			return null;
		}
		return validMules.get(Random.asInt(0, validMules.size() - 1));
	}
}
