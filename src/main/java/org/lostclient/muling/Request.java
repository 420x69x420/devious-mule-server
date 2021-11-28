package org.lostclient.muling;

import lombok.Getter;
import lombok.AllArgsConstructor;
import org.lostclient.muling.messages.client.MuleRequestMessage;

@Getter
@AllArgsConstructor
public class Request
{
	private final Client client;
	private final Client mule;
	private final MuleRequestMessage muleRequest;
}
