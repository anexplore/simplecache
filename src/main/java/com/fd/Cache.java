package com.fd;
/**
 * cache class,all methods here
 * @author caoliuyi
 *
 */
public class Cache {
	public static final long DEFAULT_MAX_SIZE = 10_000;
	private long maxSize = DEFAULT_MAX_SIZE;
	
	private volatile Element head;
	private volatile Element tail;
	
}
