package org.lbd.process;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

import org.lbd.EventBusService;
import org.lbd.messages.IPFSEvent;

import com.google.common.eventbus.EventBus;

import io.ipfs.api.IPFS;


/*
* The GNU Affero General Public License
* 
* Copyright (c) 2018 Jyrki Oraskari (Jyrki.Oraskari@gmail.f)
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

public class IPFSSubscription implements Runnable {
	private final EventBus eventBus = EventBusService.getEventBus();
	private final Supplier<Object> sub;

	public IPFSSubscription(IPFS ipfs,String channel_name) throws IOException {
		this.sub = ipfs.pubsub.sub(channel_name);
	}

	public void run() {
		while (true) {
			LinkedHashMap lhm=(LinkedHashMap) sub.get();
			Object encoded_data = lhm.get("data");
			if (encoded_data != null) {
				byte[] decoded = Base64.getDecoder().decode(encoded_data.toString());
				eventBus.post(new IPFSEvent(new String(decoded)));
			}

		}

	}
}