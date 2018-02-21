package org.lbd;

import java.io.IOException;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class IrcPingClient extends ListenerAdapter {
	private long sent_time;

	boolean is_sent = false;

	private IrcPingClient() throws InterruptedException, IOException {
		Configuration configuration = new Configuration.Builder().setName("IPFS" + (System.currentTimeMillis() % 9999))
				.addServer("irc.portlane.se").addAutoJoinChannel("#bimnetwork").addListener(this).buildConfiguration();

		PircBotX bot = new PircBotX(configuration);
		try {
			bot.startBot();
		} catch (IrcException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onJoin(JoinEvent event) {
		System.out.println("onJoin:" + event.getUser().getLogin().toString());

		if (!is_sent) {
			event.getChannel().send().message("ipfs_123");
			this.sent_time = System.currentTimeMillis();
			is_sent = true;
		}
	}

	@Override
	public void onGenericMessage(GenericMessageEvent event) {
		if (event.getMessage().startsWith("answer_")) {
			System.out.println("Got answer in : " + (System.currentTimeMillis() - this.sent_time));
		}
	}

	public static void main(String[] args) {
		try {
			new IrcPingClient();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}
