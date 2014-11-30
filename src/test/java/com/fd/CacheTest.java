package com.fd;

import junit.framework.TestCase;


public class CacheTest extends TestCase {
	public void test() throws Exception {
		final Element a = new Element("a","a");
		final Element b = new Element("b","b");
		final Element c = new Element("c","c");
		final Cache cache = new Cache("123",2);
		cache.put(a);
		cache.put(b);
		System.out.println(cache.toString());
		assertTrue(cache.getCurrentSize() == 2);
		cache.put(c);
		System.out.println(cache.toString());
		assertTrue(cache.getCurrentSize() == 2);
		assertTrue(cache.get("a") == null);
		assertTrue(cache.get("b") != null);
		System.out.println(cache.toString());
		cache.put(a);
		System.out.println(cache.toString());
		assertTrue(cache.get("c") == null);
		Thread threads[] = new Thread[20];
		for (int i = 0; i < 20; i++) {
			threads[i] = new Thread( ){
				public void run() {
					for (int i = 0; i < 100; i++) {
						cache.put(b);
						System.out.println(cache.toString());
					}
				}
			};
			threads[i].start();
		}
		for (int i = 0; i < 20; i++) {
			threads[i].join();
		}
		System.out.println(cache.toString());
	}
}
