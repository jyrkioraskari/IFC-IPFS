package org.lbd;


public class RDFClassCounter implements Comparable<RDFClassCounter> {
	private long count = 0;
	private final String URI;

	public RDFClassCounter(String uRI) {
		super();
		this.URI = uRI;
	}

	public long getCount() {
		return count;
	}

	public void inc() {
		this.count++;
	}

	public String getURI() {
		return this.URI;
	}

	@Override
	public int compareTo(RDFClassCounter o) {
		return -Long.compare(this.count, o.count);
	}

	
	
}
