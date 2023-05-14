package org.lostclient.muling.messages.server;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;
import org.lostclient.muling.messages.MuleTile;

public class MuleResponseMessage extends AbstractMessage
{
	public final boolean success;
	public final String errorMessage;
	public final int world;
	public final MuleTile location;

	public MuleResponseMessage(boolean success, String errorMessage, int world, MuleTile location)
	{
		super(MessageType.MULE_RESPONSE);
		this.success = success;
		this.errorMessage = errorMessage;
		this.world = world;
		this.location = location;
	}
}
