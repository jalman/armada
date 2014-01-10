package team027.messaging;

import static team027.utils.Utils.ALLY_TEAM;
import static team027.utils.Utils.RC;
import static team027.utils.Utils.isFirstRound;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

/**
 * Securely send and receive messages.
 * @author vlad
 */
public class MessagingSystem {

	public static final int MESSAGE_SIZE = 6;
	public static final int BLOCK_SIZE = 1 + MESSAGE_SIZE / 2;

	private static final int MAX_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 1024;
	private static final int COPIES = 2;
	private static final int DISPLACEMENT = MAX_CHANNEL / COPIES;

	private static final int MASK = (1 << 16) - 1;

	private static final MessageType[] MESSAGE_TYPE = MessageType.values();

	public static final double MESSAGING_COST = 10;

	public static final int HQ = RobotType.HQ.ordinal(),
      SOLDIER = RobotType.SOLDIER.ordinal();

	/**
	 * The channels of communication. These are set each round.
	 */
	public final int[] channels = new int[COPIES];

	/**
	 * Stores messages that are read each round.
	 */
	//public final int[][] buffer = new int[500][MESSAGE_SIZE];

	/**
	 * The total number of messages posted by our team. Includes the header message.
	 */
	public int total_messages;
	/**
	 * The number of valid messages read this round.
	 */
	//public int valid_messages;
	/**
	 * Whether any messages were written this round.
	 */
	private boolean message_written;

	/**
	 * Whether we want to send messages this round.
	 */
	private final boolean send_messages = true;

	public MessagingSystem() {}

	public int key(int seed) {
		Random r = new Random(seed);
		return r.nextInt();
	}

	/**
	 * Sets up the channels for communication. Should be called each round.
	 */
	private void setChannels() {
		int k = key(Clock.getRoundNum() ^ (ALLY_TEAM.ordinal() << 16));
		for(int i = 0; i < COPIES; i++) {
			channels[i] = (k + i * DISPLACEMENT) % MAX_CHANNEL;
			if(channels[i] < 0) channels[i] += MAX_CHANNEL;
		}
	}

	/**
	 * Reads a block from the global message board.
	 * @param channel The location of the block.
	 * @param block Stores the block.
	 * @return The message type, or -1 if corrupted.
	 * @throws GameActionException
	 */
	public int readBlock(int channel, int[] block) throws GameActionException {
		int header = RC.readBroadcast(channel++);
		int type = header & MASK;
		if(type >= MESSAGE_TYPE.length) return -1;

		short check = (short) ((header >> 16) - type);

		final int length = MESSAGE_TYPE[type].length;

		for(int i = 0; i < length / 2; i++) {
			int data = RC.readBroadcast(channel++);

			check -= (block[2*i] = (short)(data & MASK));
			check -= (block[2*i+1] = (short)(data >> 16));
		}

		if(length % 2 == 1) {
			check -= (block[length-1] = (short)(RC.readBroadcast(channel) & MASK));
		}

		if(check == 0) {
			return type;
		} else {
			return -1;
		}
	}

	/**
	 * Checks whether a block is corrupted.
	 * @param channel The location of the block.
	 * @param type The type of message we expect.
	 * @return Whether the channel is corrupted.
	 * @throws GameActionException
	 */
	private boolean checkBlock(int channel, int type) throws GameActionException {
		int header = RC.readBroadcast(channel++);
		if((header & MASK) != type) return false;

		short check = (short)((header >> 16) - type);
		int length = (MESSAGE_TYPE[type].length+1)/2;

		for(int i = 0; i < length; i++) {
			int data = RC.readBroadcast(channel++);
			check -= (short)(data >> 16) + (short)(data & MASK);
		}

		return check == 0;
	}

	/**
	 * Tries to read a single message, using multiple channels of communication.
	 * If an uncorrupted copy is found, the other copies are checked and fixed.
	 * @param index Index of the message among the set of messages.
	 * @param block Stores the message.
	 * @return The message type, or -1 if corrupted.
	 * @throws GameActionException
	 */
	private int readMessage(int index, int[] block) throws GameActionException {
		int off = index * BLOCK_SIZE;
		for(int i = 0; i < COPIES; i++) {
			int type = readBlock(channels[i]+off, block);
			if(type != -1) {

				//fix messages
				for(int j = 0; j < i; j++) {
					writeBlock(channels[j]+off, type, block);
//					System.out.println("Fixing message " + j);
				}

				for(int j = i+1; j < COPIES; j++) {
					if(!checkBlock(channels[j]+off, type)) {
						writeBlock(channels[j]+off, type, block);
//						System.out.println("Fixing message " + j);
					}
				}

				return type;
			}
		}
		return -1;
	}

	/**
	 * Reads the messages posted by our team this round.
	 * This method should be called once per turn.
	 * @throws GameActionException
	 */
	private void readMessages(MessageHandler[] handlers) throws GameActionException {
		int[] buffer = new int[MESSAGE_SIZE];
		if(readMessage(0, buffer) != 0) {
			System.out.println("Cannot read header message!");
			return;
		}

		int new_messages = buffer[0];

		for(int i = total_messages; i < new_messages; i++) {
			int type = readMessage(i, buffer);
			if(type != -1) {
				try {
					handlers[type].handleMessage(buffer);
				} catch(Exception e) {
					printMessageBoard();
					//e.printStackTrace();
				}
			} else {
				System.out.println("Cannot read message at index " + i);
			}
		}

		total_messages = new_messages;
	}

	private void writeBlock(int channel, int type, int[] block) throws GameActionException {
		int checksum = type;
		final int length = MESSAGE_TYPE[type].length;

		for(int i = 0; i < length; i++) {
			checksum += block[i];
		}

		RC.broadcast(channel++, (checksum << 16) ^ type);

		for(int i = 0; i < length / 2; i++) {
			RC.broadcast(channel++, (block[2*i+1] << 16) ^ (block[2*i] & MASK));
		}

		if(length % 2 == 1) {
			RC.broadcast(channel, block[length-1] & MASK);
		}
	}

	/**
	 * Writes a message to the global radio.
	 * @param index The index at which to write the message.
	 * @param message The message data.
	 * @throws GameActionException
	 */
	private void writeMessageAtIndex(int index, int type, int... message) throws GameActionException {
		int off = index * BLOCK_SIZE;

		for(int i = 0; i < COPIES; i++) {
			writeBlock(channels[i]+off, type, message);
		}
		message_written = true;
	}

	/**
	 * Writes a message to the global radio.
	 * @param type The type of message.
	 * @param message The message data.
	 * @throws GameActionException
	 */
	public void writeMessage(int type, int... message) throws GameActionException {
		if(!send_messages) return;

		int off = total_messages * BLOCK_SIZE;

		for(int i = 0; i < COPIES; i++) {
			writeBlock(channels[i]+off, type, message);
		}
		total_messages++;
		message_written = true;
	}

	public void beginRound(MessageHandler[] handlers) throws GameActionException {

		//read previous round's messages
		if(!isFirstRound()) {
			readMessages(handlers);
		}

		//set channels for new round
		setChannels();

		//only the header message has been read
		total_messages = 1;

		//read new messages
		readMessages(handlers);

		message_written = false;
	}

	public void beginRoundHQ(MessageHandler[] handlers) throws GameActionException {
		//read previous round's messages
		if(!isFirstRound()) {
			readMessages(handlers);
//			printMessageBoard();
		}

		//set channels for new round
		setChannels();

		//write the header message
		total_messages = 1;
		message_written = true;
	}

	/**
	 * Rewrites the header message. Should be called at the end of each round by any robot that uses messaging.
	 * @throws GameActionException
	 */
	public void endRound() throws GameActionException {
		if(message_written) {
			writeHeaderMessage(total_messages);
		}
		message_written = false;
	}

	private void writeHeaderMessage(int total_messages) throws GameActionException {
		writeMessageAtIndex(0, MessageType.HEADER.ordinal(), total_messages);
	}

	/**
	 * Announce somewhere to attack.
	 * @param loc: place to attack
	 * @param priority: priority of attack
	 * @throws GameActionException
	 */
	public void writeAttackMessage(MapLocation loc, int priority) throws GameActionException {
		writeMessage(MessageType.ATTACK_LOCATION.ordinal(), loc.x, loc.y, priority);
	}

	/**
	 * Announce somewhere to micro.
	 * @param loc
	 * @param goIn
	 * @throws GameActionException
	 */
	public void writeMicroMessage(MapLocation loc, int goIn) throws GameActionException {
		writeMessage(MessageType.MICRO_INFO.ordinal(), loc.x, loc.y, goIn);
	}

	/**
	 * Announce a robot's birth. (Usually an encampment.)
	 * @param loc: location of birth
	 * @param id: id of new robot
	 * @param type: type of new robot
	 * @throws GameActionException
	 */
	public void writeBirthMessage(MapLocation loc, int id, int type) throws GameActionException {
		writeMessage(MessageType.BIRTH_INFO.ordinal(), loc.x, loc.y, id, type);
	}

  //
  // /**
  // * Announce the strategy.
  // * @param strategy: strategy to execute
  // * @throws GameActionException
  // */
  // public void writeStrategy(Strategy strategy) throws GameActionException {
  // writeMessage(MessageType.STRATEGY.ordinal(), strategy.ordinal());
  // }
  //
  // /**
  // * Announce the strategy parameters.
  // * @param parameters Parameters which guide robot behavior.
  // * @throws GameActionException
  // */
  // public void writeParameters(Parameters parameters) throws GameActionException {
  // writeMessage(MessageType.PARAMETERS.ordinal(), parameters.greed, (int)(parameters.border *
  // 1024));
  // }

	/**
	 * ???
	 * @param pointNumber
	 * @throws GameActionException
	 */
	public void writeCheckpointMessage(int pointNumber) throws GameActionException {
		writeMessage(MessageType.CHECKPOINT_NUMBER.ordinal(), pointNumber);
	}

	/**
	 * Announce the attempted taking of an encampment.
	 * @param loc The location of the encampment.
	 * @param appeal How appealing this task is to this robot.
	 * @throws GameActionException
	 */
	public void writeTakingEncampmentMessage(MapLocation loc, int appeal) throws GameActionException {
		writeMessage(MessageType.TAKING_ENCAMPMENT.ordinal(), loc.x, loc.y, appeal);
	}

	/**
	 * Announce the intent to take an encampment.
	 * @param loc: location of encampment
	 * @param priority: priority
	 * @param buildType: type of encampment to build
	 * @throws GameActionException
	 */
	public void writeTakeEncampmentMessage(MapLocation loc, int priority, RobotType buildType) throws GameActionException {
		writeMessage(MessageType.TAKE_ENCAMPMENT.ordinal(), loc.x, loc.y, priority, buildType.ordinal());
	}

	/**
	 * Announce the [attempted] laying of [a] mine[s].
	 * @param loc location of mine (or center of 5 mines)
	 * @throws GameActionException
	 */
	public void writeLayingMineMessage(MapLocation loc) throws GameActionException {
		writeMessage(MessageType.LAYING_MINE.ordinal(), loc.x, loc.y);
	}

	/**
	 * Announce the [attempted] defusing of a mine.
	 * @param loc The location of the mine.
	 * @param appeal How appealing this task is to this robot.
	 * @throws GameActionException
	 */
	public void writeDefusingMineMessage(MapLocation loc, int appeal) throws GameActionException {
		writeMessage(MessageType.DEFUSING_MINE.ordinal(), loc.x, loc.y, appeal);
	}

	/**
	 * Announce an upgrade from the HQ. Should only be called by HQ.
	 * @param upgradeId = Upgrade.TYPE.ordinal()
	 * @throws GameActionException
	 */
	public void writeAnnounceUpgradeMessage(int upgradeId) throws GameActionException {
		writeMessage(MessageType.ANNOUNCE_UPGRADE.ordinal(), upgradeId);
	}

	/**
	 * Announce location of shield to a soldier.
	 * @param loc: location of shield.
	 * @throws GameActionException
	 */
	public void writeShieldLocationMessage(MapLocation loc) throws GameActionException {
		writeMessage(MessageType.SHIELD_LOCATION.ordinal(), loc.x, loc.y);
	}

	/**
	 * Announce robot's sequential SOLDIER_ID. Only called by HQ.
	 * @param soldierID: soldier ID to assign.
	 * @throws GameActionException
	 */
	public void writeSoldierID(int soldierID) throws GameActionException {
		writeMessage(MessageType.SOLDIER_ID.ordinal(), soldierID);
	}

	public void printMessageBoard() {
//		System.out.println("MESSAGEBOARD:");
		for(int i = 0; i < total_messages; i++) {
			int off = i * BLOCK_SIZE;
			for(int j = 0; j < BLOCK_SIZE; j++) {
				try {
					int broadcast = RC.readBroadcast(channels[0] + off + j);
					System.out.print((broadcast >> 16) + " " + (broadcast & MASK) + " ");
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
			System.out.println();
		}
	}



	public void scramble(int start, int end) {
		start = start < 0 ? 0 : start;
		end = end >= GameConstants.BROADCAST_MAX_CHANNELS ? GameConstants.BROADCAST_MAX_CHANNELS : end;
		for(int i = start; i < end; i++) {
			if(!isAChannel(i)) {
				try {
					RC.broadcast(i,0);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean isAChannel(int i) {
		for(int j = 0; j < channels.length; j++) {
			if(channels[i] == i) {
				return true;
			}
		}
		return false;
	}
}
