package microbot2.messaging;

public class CastTest {

	static final int MASK = (1 << 16) - 1;
	
	public static void main(String[] args) {
		short s1 = -1234;
		short s2 = -1;
		int i = shorts2Int(s1, s2);
		
		System.out.println(short1(i));
		System.out.println(short2(i));
		
		int i1 = -1234;
		int i2 = -1;
		i = ints2Int(i1, i2);
		
		System.out.println(int1(i));
		System.out.println(int2(i));
	}
	
	public static int shorts2Int(short s1, short s2) {
		return ((int)s1 << 16) ^ (s2 & MASK);
	}
	
	public static short short1(int i) {
		return (short)(i >> 16);
	}
	
	public static short short2(int i) {
		return (short)(i & MASK);
	}
	
	public static int ints2Int(int s1, int s2) {
		return ((int)s1 << 16) ^ (s2 & MASK);
	}
	
	public static int int1(int i) {
		return (i >> 16);
	}
	
	public static int int2(int i) {
		return (short)(i & MASK);
	}

}
