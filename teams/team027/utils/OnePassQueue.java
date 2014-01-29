package team027.utils;

public class OnePassQueue<T> {

  private final T[][] queue;
  private final int[] length;
  public int min = 0;
  public int size = 0;

  @SuppressWarnings("unchecked")
  public OnePassQueue(int max_key, int max_length) {
    queue = (T[][]) new Object[max_key][max_length];
    length = new int[max_key];
  }

  public void insert(int key, T value) {
    if (key < min) {
      min = key;
      // throw new ArrayIndexOutOfBoundsException("Attempted to insert " + key + " < " + min);
    }
    queue[key][length[key]++] = value;
    size++;
  }

  /**
   * Does not check key < min.
   */
  public void insert2(int key, T value) {
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
