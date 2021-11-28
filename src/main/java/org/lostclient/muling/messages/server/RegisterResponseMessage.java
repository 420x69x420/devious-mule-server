package org.lostclient.muling.messages.server;

import org.lostclient.muling.messages.AbstractMessage;
import org.lostclient.muling.messages.MessageType;

public class RegisterResponseMessage extends AbstractMessage
{
	public final boolean success;

	public RegisterResponseMessage(boolean success)
	{
		super(MessageType.REGISTER_RESPONSE);
		this.success = success;
	}
}
