package org.lbd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.IPFS_Fetch;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;

public class IPFS_Repeat100ConcurrentFetchDir extends IPFS_Fetch {

	private final Property merkle_node;

	private final IPFS ipfs;
	private final Model guid_directory_model = ModelFactory.createDefaultModel();
	private final List<Model> model_list = new ArrayList<>();
	private int active_model = 0;
	private boolean clean = false;

	public IPFS_Repeat100ConcurrentFetchDir(boolean clean, String dir_hash) throws IOException {
		this.ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001"); // 5002 for Seppo
		this.merkle_node = guid_directory_model.createProperty("http://ipfs/merkle_node");
		this.clean = clean;
		this.node = dir_hash;

		for (int i = 0; i < 400; i++) {
			model_list.add(ModelFactory.createDefaultModel());
		}

		fetch(dir_hash, true);
		timelog.stream().forEach(txt -> writeToFile(txt, true));
		timelog.clear();
		fetch(dir_hash, false);
		timelog.stream().forEach(txt -> writeToFile(txt, false));
	}

	public void fetch(String dir_hash, boolean jena) {
		ExecutorService taskExecutor1 = Executors.newFixedThreadPool(100);
		ExecutorService taskExecutor2 = Executors.newFixedThreadPool(100);
		DateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");

		Date today = Calendar.getInstance().getTime();
		this.reportDate = df.format(today);
		System.out.println("Read DIR");
		readInGuidTable(dir_hash);
		long start;
		long end;
		if (this.clean) {
			System.out.println("DIR read");
			this.guid_directory_model.listStatements().toList().stream()
					.filter(s -> s.getPredicate().equals(this.merkle_node)).map(s -> s.getObject()).forEach(x -> {
						Multihash filePointer = Multihash.fromBase58(x.asLiteral().getLexicalForm());
						try {
							ipfs.pin.rm(filePointer);
						} catch (Exception e) {

						}
					});
			try {
				ipfs.repo.gc();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Remove done");

			System.out.println("START");

			start = System.nanoTime();
			this.guid_directory_model.listStatements().toList().stream()
					.filter(s -> s.getPredicate().equals(this.merkle_node)).map(s -> s.getObject()).forEach(x -> {
						Future<Integer> future = taskExecutor1.submit(createreadNodeCallable(
								model_list.get(getNextJenaModel()), x.asLiteral().getLexicalForm(), jena));
					});

			taskExecutor1.shutdown();
			try {
				taskExecutor1.awaitTermination(1, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			end = System.nanoTime();
			System.out.println("First round read in: total " + (end - start) / 1000000f + " ms");
			addLog("First round read in: total " + (end - start) / 1000000f + " ms");

		}
		System.gc();
		start = System.nanoTime();

		this.guid_directory_model.listStatements().toList().stream()
				.filter(s -> s.getPredicate().equals(this.merkle_node)).map(s -> s.getObject()).forEach(x -> {
					Future<Integer> future = taskExecutor2.submit(createreadNodeCallable(
							model_list.get(getNextJenaModel()), x.asLiteral().getLexicalForm(), jena));
				});
		taskExecutor2.shutdown();
		try {
			taskExecutor2.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		end = System.nanoTime();
		System.out.println("Second round read in: total " + (end - start) / 1000000f + " ms");
		addLog("Second round read in: total " + (end - start) / 1000000f + " ms");

	}

	private int getNextJenaModel() {
		return active_model++;
	}

	private Callable<Integer> createreadNodeCallable(Model model, String key, boolean jena) {
		return () -> {
			readInNode(model, key, jena);
			return 123;

		};
	}

	private void readInNode(Model temp_model, String key, boolean jena) {
		long start = System.nanoTime();
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			String tmp = new String(ipfs.cat(filePointer));
			long end = System.nanoTime();
			addLog(key + " read  in: " + (end - start) / 1000000f + " ms");
			ByteArrayInputStream bi = new ByteArrayInputStream(tmp.getBytes());
			if (jena)
				temp_model.read(bi, null, "TTL");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readInGuidTable(String key) {
		guid_directory_model.removeAll();
		Multihash filePointer = Multihash.fromBase58(key);
		try {
			String content = new String(ipfs.cat(filePointer));
			guid_directory_model.read(new ByteArrayInputStream(content.getBytes()), null, "TTL");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		String hash = "QmeprUL7X1s5T8DZQUgfbuRGxiWBaA5jzqGEUMssuSaurY";
		for (int n = 0; n < 100; n++) {
			try {
				if (args.length == 0)
					new IPFS_Repeat100ConcurrentFetchDir(true, hash);
				else if (args[0].toLowerCase().trim().equals("true"))
					new IPFS_Repeat100ConcurrentFetchDir(true, hash);
				else
					new IPFS_Repeat100ConcurrentFetchDir(false, hash);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}