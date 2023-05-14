package org.lostclient.muling;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.lostclient.muling.messages.client.MuleRequestMessage;

@Getter
@RequiredArgsConstructor
public class Request
{
	private final Client client;
	private final Client mule;
	private final MuleRequestMessage muleRequest;

	@Setter
	private boolean completed;
}
