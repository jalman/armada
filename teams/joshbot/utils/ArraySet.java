package joshbot.utils;

public class ArraySet<T> {
	private Object[] array;
	public int size;
	
	public ArraySet(int capacity) {
		array = new Object[capacity];
		size = 0;
	}
	
	public ArraySet(T... ts) {
		array = ts;
		size = ts.length;
	}
	
	public void insert(T t) {
		array[size++] = t;
	}
	
	public T get(int index) {
		return (T) array[index];
	}
	
	public void set(int index, T t) {
		array[index] = t;
	}
	
	public void delete(int index) {
		if(--size > 0) {
			array[index] = array[size];
		}
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public void debug() {
		System.out.println(this.toString());
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < size; i++) {
			sb.append(", ");
			sb.append(array[i]);
		}
		return sb.toString();
	}
}
