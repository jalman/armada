package examplejurgzplayer.messaging;

public enum MessageType {

	HEADER(1), 
	STRATEGY(1),
	PARAMETERS(2),
	ATTACK_LOCATION(3), 
	TASK_TAKEN(3),  
	MICRO_INFO(3), 
	BIRTH_INFO(4), 
	SOLDIER_ID(1);

	/**
	 * Number of integers that comprise this message.
	 */
	public final int length;
	
	private MessageType(int length) {
		this.length = length;
	}
}
