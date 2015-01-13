package com.fd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * cache class,all methods here
 * 
 * @author caoliuyi
 *
 */
public class Cache {
	public static final int DEFAULT_MAX_SIZE = 10_000;
	private int maxSize = DEFAULT_MAX_SIZE;
	private String cacheName = "";
	private volatile Element head;
	private volatile Element tail;
	private final AtomicLong currentNum = new AtomicLong(0);
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock rLock = lock.readLock();
	private final Lock wLock = lock.writeLock();
	private final Map<Object, Element> container;

	public Cache(String name) {
		this(name, DEFAULT_MAX_SIZE);
	}

	public Cache(String name, int maxSize) {
		if (name == null) {
			throw new RuntimeException("name can not be null");
		}
		this.maxSize = maxSize;
		cacheName = name;
		container = new HashMap<Object, Element>(maxSize);
		head = null;
		tail = null;

	}

	/**
	 * put element to cache
	 * 
	 * @param ele
	 *            , not null
	 * @return
	 */
	public boolean put(Element ele) {
		if (ele == null) {
			return false;
		}
		Element tmp = ele.clone();
		if (tmp.isExpired()) {
			return false;
		}
		wLock.lock();
		try {
			if (tmp.isExpired()) {
				return false;
			}
			if (container.containsKey(tmp.key)) {
				remove(container.get(tmp.key));
			}
			long size = getCurrentSize();
			if (size == maxSize) {
				removeLast();
			}
			container.put(tmp.key, tmp);
			moveToHead(tmp);
			currentNum.incrementAndGet();
		} finally {
			wLock.unlock();
		}
		return true;
	}

	/**
	 * get element from cache
	 * 
	 * @param key
	 * @return
	 */
	public Element get(Object key) {
		if (key == null) {
			return null;
		}
		Element e = null;
		rLock.lock();
		try {
			if (!container.containsKey(key)) {
				return null;
			}
			e = container.get(key);
			if (!e.isExpired()) {
				rLock.unlock();
				wLock.lock();
				if (container.containsKey(key)) {
					e.increaseHitCount();
					moveToHead(e);
					e = e.clone();
				} else {
					e = null;
				}
				rLock.lock();
				wLock.unlock();
			} else {
				rLock.unlock();
				wLock.lock();
				remove(e);
				rLock.lock();
				wLock.unlock();
				e = null;
			}
			return e;
		} finally {
			rLock.unlock();
		}
	}

	private void moveToHead(Element ele) {
		if (ele == null) {
			return;
		}
		if (head == null) {
			tail = head = ele;
			head.setPre(tail);
			tail.setNext(head);
			return;
		}
		if (head == ele) {
			return;
		}
		if (tail == ele) {
			tail = tail.getPre();
		}
		head.setPre(ele);
		ele.setNext(head);
		head = ele;
		tail.setNext(head);
		head.setPre(tail);

	}

	private void removeLast() {
		if (tail == null) {
			return;
		}
		container.remove(tail.key);
		if (tail == head) {
			head = tail = null;
		} else {
			Element pre = tail.getPre();
			pre.setNext(head);
			head.setPre(pre);
			tail.setNext(null);
			tail.setPre(null);
			tail = pre;
		}
		currentNum.decrementAndGet();
	}

	private void remove(Element ele) {
		if (ele == null) {
			return;
		}
		if (head == ele && head == tail) {
			removeLast();
		}
		if (head == ele) {
			Element next = head.getNext();
			tail.setNext(next);
			next.setPre(tail);
			head.setPre(null);
			head.setNext(null);
			head = next;
			container.remove(ele.key);
			currentNum.decrementAndGet();
		} else if (tail == ele) {
			removeLast();
		} else {
			ele.getPre().setNext(ele.getNext());
			ele.getNext().setPre(ele.getPre());
			container.remove(ele.key);
			currentNum.decrementAndGet();
		}
	}

	/**
	 * current number of element in cache
	 * 
	 * @return
	 */
	public long getCurrentSize() {
		return currentNum.get();
	}

	public String getCacheName() {
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public String toString() {
		rLock.lock();
		StringBuilder sb = new StringBuilder();
		try {
			Element start = head;
			while (start != null) {
				sb.append(start.getValue().toString() + "\n");
				start = start.getNext();
				if (start == head) {
					break;
				}
			}
			return sb.toString();
		} finally {
			rLock.unlock();
		}
	}
}
