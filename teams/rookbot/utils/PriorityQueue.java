package rookbot.utils;

import rookbot.utils.PriorityQueue.Node;

public interface PriorityQueue<V, N extends Node<V>> {
	public N insert(int key, V value);
	public V deleteMin();
	public void decreaseKey(N node, int key);
	public int size();
	
	public static class Node<V> {
		int key;
		V value;
		
		public Node(int k, V v) {
			key = k;
			value = v;
		}
	}
}
