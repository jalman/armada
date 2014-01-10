package team027.utils;

public class OnePassQueue<T> {
	
	private T[][] queue;
	private int[] length;
	public int min = 0;
	public int size = 0;
	
	@SuppressWarnings("unchecked")
	public OnePassQueue(int max_key, int max_size) {
		queue = (T[][]) new Object[max_key][max_size];
		length = new int[max_key];
	}
	
	public void insert(int key, T value) {
		queue[key][length[key]++] = value;
		size++;
	}
	
	public T deleteMin() {
		while(length[min] == 0) {
			min++;
		}
		size--;
		return queue[min][--length[min]];
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		for(int key = 0; key < queue.length; key++) {
			for(int i = 0; i < length[key]; i++) {
				s.append(queue[key][i]).append(" ");
			}
		}
		s.append('\n');
		return s.toString();
	}
}
