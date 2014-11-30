package com.fd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * 缓存中的每个条目
 * 
 * @author caoliuyi
 *
 */
class Element implements Serializable, Cloneable {
	private static final long serialVersionUID = 1098572221246444544L;
	private static final long DEFAULT_TTL = 120_000;

	public final Object key;
	public final Object value;
	public volatile long ttl = DEFAULT_TTL;// 存活时间 ms
	private volatile long hitCount = 0;// 命中次数
	private transient long creationTime;// 创建时间

	private volatile Element pre;// 链表中前一个元素
	private volatile Element next;// 链表中后一个元素

	private final AtomicLongFieldUpdater<Element> hitCountUpdater = 
			AtomicLongFieldUpdater.newUpdater(Element.class, "hitCount");
	
	public Element(Object key, Object value) throws Exception {
		if (key == null) {
			throw new Exception("key cannot be null");
		}
		this.key = key;
		this.value = value;
		this.creationTime = getCurrentTime();
	}

	public Element(Object key, Object value, long timeToLive) throws Exception {
		this(key,value);
		this.ttl = timeToLive;
	}
	
	
	/**
	 * @return if has reached ttl
	 */
	public boolean isExpired() {
		return getCurrentTime() - creationTime > ttl;
	}

	/**
	 * 
	 * @return default timeToLive value
	 */
	public long getDefaultTtl() {
		return DEFAULT_TTL;
	}
	/**
	 * 
	 * @return time to live
	 */
	public long getTtl() {
		return ttl;
	}
	protected void setHitCount(long hit) {
		hitCountUpdater.set(this, hit);
	}
	public void increaseHitCount() {
		hitCountUpdater.incrementAndGet(this);
	}
	public long getHitCount() {
		return hitCount;
	}
	protected Element getPre() {
		return pre;
	}
	protected void setPre(Element pre) {
		this.pre = pre;
	}
	protected Element getNext() {
		return next;
	}
	protected void setNext(Element next) {
		this.next = next;
	}
	protected void updateCreationTime() {
		this.creationTime = getCurrentTime();
	}
	
	public Object getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	/**
	 * if error,return null
	 * @return cloned element 
	 */
	public Element clone() {
		return deepClone();
	}	
	/**
	 * if error,return null
	 * 
	 * @return
	 */
	private Element deepClone() {
		try {
			return new Element(deepCopy(key), deepCopy(value), ttl);
		} catch (Exception ignore) {
		}
		return null;
	}

	private static Object deepCopy(final Object oldValue) throws IOException,
			ClassNotFoundException {
		Serializable newValue = null;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			oos = new ObjectOutputStream(bout);
			oos.writeObject(oldValue);
			ByteArrayInputStream bin = new ByteArrayInputStream(
					bout.toByteArray());
			ois = new ObjectInputStream(bin);
			newValue = (Serializable) ois.readObject();
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
				if (ois != null) {
					ois.close();
				}
			} catch (Exception e) {
			}
		}
		return newValue;
	}
	
	private long getCurrentTime() {
		return System.currentTimeMillis();
	}
	public String toString() {
		return key.toString() +"," + value.toString();
	}
}
