package vladbot.utils;

import vladbot.utils.BucketPriorityQueue.BucketNode;
import vladbot.utils.PriorityQueue.Node;

/**
 * A one-pass priority queue using buckets.
 * @author vlad
 *
 * @param <V> The value type.
 */
public class BucketPriorityQueue<V> implements PriorityQueue<V, BucketNode<V>> {

	public static void main(String[] args) {
		BucketPriorityQueue<Object> queue = new BucketPriorityQueue<Object>(500);
		
		BucketNode<Object>[] nodes = new BucketNode[50];
		
		for(int i = 0; i < nodes.length; i++) {
			nodes[i] = queue.insert(10*i, new Integer(i));
		}
		
		queue.decreaseKey(nodes[20], 105);
		
		while(queue.size() > 0) {
			System.out.println(queue.deleteMin());
		}		
	}
	
	private final BucketNode<V>[] queue;
	public int size;
	private int min;
	
	@SuppressWarnings("unchecked")
	public BucketPriorityQueue(int max_key) {
		queue = new BucketNode[max_key];
		size = 0;
		min = 0;
	}
	
	public int size() {return size;}
	
	public static class BucketNode<V> extends Node<V> {
		private BucketNode<V> prev, next;
		public BucketNode(int k, V v) {
			super(k, v);
		}
		
		public String toString() {
			return "(" + key + ", " + value + ")";
		}
	}

	@Override
	public BucketNode<V> insert(int key, V value) {
		BucketNode<V> node = new BucketNode<V>(key, value);
		insert(node);
		return node;
	}

	private void insert(BucketNode<V> node) {
		BucketNode<V> head = queue[node.key];
		if(head == null) {
			queue[node.key] = node.prev = node.next = node;
		} else {
			node.next = head.next;
			head.next = node;
			node.next.prev = node;
			node.prev = head;
		}
		size++;
	}
	
	@Override
	public V deleteMin() {
		if(size == 0) return null;
		
		while(queue[min] == null) {
			min++;
		}
		
		BucketNode<V> node = queue[min];
		delete(node);
		return node.value;
	}
	
	private void delete(BucketNode<V> node) {
		if(node.next == node) {
			queue[node.key] = null;
		} else {
			node.next.prev = node.prev;
			node.prev.next = node.next;
		}
		size--;
	}

	/**
	 * It is crucial that this method does not decrease the key to below
	 * that from the last call to deleteMin().
	 */
	@Override
	public void decreaseKey(BucketNode<V> node, int key) {
		delete(node);
		node.key = key;
		insert(node);
	}
	
	public void debug() {		
		int num = 0;

		for(int key = 0; num < size; key++) {
			BucketNode<V> head = queue[key];
			if(head != null) {
				System.out.print(head + " ");
				num++;
				for(BucketNode<V> node = head.next; node != head; node = node.next) {
					System.out.print(node  + " ");
					num++;
				}
			}
		}
		System.out.println();
	}
}
