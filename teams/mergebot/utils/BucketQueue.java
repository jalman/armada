package mergebot.utils;

import java.util.Arrays;

public class BucketQueue<T> {

  private final T[][] queue;
  private final int[] length;
  public int min = 0;
  public int size = 0;

  @SuppressWarnings("unchecked")
  public BucketQueue(int max_key, int max_length) {
    queue = (T[][]) new Object[max_key][max_length];
    length = new int[max_key];
  }

  public void insert(int key, T value) {
    if (key < min) {
      min = key;
      // throw new ArrayIndexOutOfBoundsException("Attempted to insert " + key + " < " + min);
    }
    T[] q = queue[key];
    if (length[key] == q.length) {
      // System.out.println("Increasing queue length (" + q.length + ") for " + key);
      q = Arrays.copyOf(q, 10 * q.length);
    }
    q[length[key]++] = value;
    queue[key] = q;
    size++;
  }

  /**
   * Does not check key < min or increase queue lengths.
   */
  public void insert_fast(int key, T value) {
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

  @Override
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
