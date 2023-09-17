package org.lostclient.muling.server;

public class Main
{
	public static void main(String[] args)
	{
		Server server = new Server(42067);
		server.start();
	}
}
