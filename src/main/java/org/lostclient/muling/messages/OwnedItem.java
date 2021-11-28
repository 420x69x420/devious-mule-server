package org.lostclient.muling.messages;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class OwnedItem
{
	private final int itemId;
	private final int quantity;
}
