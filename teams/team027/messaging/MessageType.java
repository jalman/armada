package team027.messaging;

public enum MessageType {

	HEADER(1), 
	STRATEGY(1),
	PARAMETERS(2),
	ATTACK_LOCATION(3), 
	CHECKPOINT_NUMBER(1), 
	TASK_TAKEN(3),  
	TAKING_ENCAMPMENT(3), 
	TAKE_ENCAMPMENT(4), 
	MICRO_INFO(3), 
	BIRTH_INFO(4), 
	LAYING_MINE(2),
	DEFUSING_MINE(3),
	ANNOUNCE_UPGRADE(1),
	SHIELD_LOCATION(2),
	SOLDIER_ID(1);

	/**
	 * Number of integers that comprise this message.
	 */
	public final int length;
	
	private MessageType(int length) {
		this.length = length;
	}
}
