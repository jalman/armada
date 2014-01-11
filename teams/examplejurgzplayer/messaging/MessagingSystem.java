package examplejurgzplayer.messaging;

import static examplejurgzplayer.messaging.MessageType.MESSAGE_TYPES;
import static examplejurgzplayer.utils.Utils.RC;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

/**
 * Efficiently send and receive messages.
 * @author vlad
 */
public class MessagingSystem {

  /**
   * Needs to be larger than any message size.
   */
  public static final int MESSAGE_SIZE = 6;
  public static final int BLOCK_SIZE = 1 + MESSAGE_SIZE;

  private static final int HEADER_MESSAGE_INDEX = GameConstants.BROADCAST_MAX_CHANNELS / BLOCK_SIZE;
  private static final int MAX_MESSAGE_INDEX = HEADER_MESSAGE_INDEX;

  public static final double BROADCAST_COST = 10;

  public static final int HQ = RobotType.HQ.ordinal(),
      SOLDIER = RobotType.SOLDIER.ordinal();

  /**
   * The total number of messages posted by our team. Includes the header message.
   */
  public int message_index;

  /**
   * Whether any messages were written this round.
   */
  private boolean message_written;

  /**
   * Whether we want to send messages this round.
   */
  private final boolean send_messages = true;

  public MessagingSystem() {}

  /**
   * Reads a single message from the global message board.
   * @param index Index of the message among the set of messages.
   * @param block Stores the message.
   * @return The message type, or -1 if corrupted.
   * @throws GameActionException
   */
  private int readMessage(int index, int[] block) throws GameActionException {
    int channel = (index % MAX_MESSAGE_INDEX) * BLOCK_SIZE;

    int type = RC.readBroadcast(channel++);
    final int length = MESSAGE_TYPES[type].length;

    for (int i = 0; i < length; i++) {
      block[i] = RC.readBroadcast(channel++);
    }
    return type;
  }

  /**
   * Reads the messages posted by our team this round.
   * This method should be called once per turn.
   * @throws GameActionException
   */
  private void readMessages(MessageHandler[] handlers) throws GameActionException {
    int[] buffer = new int[MESSAGE_SIZE];
    if (readMessage(HEADER_MESSAGE_INDEX, buffer) != MessageType.HEADER.ordinal()) {
      System.out.println("Cannot read header message!");
      return;
    }

    int new_index = buffer[0];

    for (int index = message_index; index < new_index; index++) {
      int type = readMessage(index, buffer);
      handlers[type].handleMessage(buffer);
    }

    message_index = new_index;
  }

  /**
   * Writes a message to the global radio.
   * @param index The index at which to write the message.
   * @param message The message data.
   * @throws GameActionException
   */
  private void writeMessageAtIndex(int index, int type, int... message) throws GameActionException {
    int channel = (index % MAX_MESSAGE_INDEX) * BLOCK_SIZE;

    RC.broadcast(channel++, type);

    for (int i = 0; i < message.length; i++) {
      RC.broadcast(channel++, message[i]);
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
    if (!send_messages) return;

    writeMessageAtIndex(message_index++, type, message);

    message_written = true;
  }

  public void beginRound(MessageHandler[] handlers) throws GameActionException {
    readMessages(handlers);
    message_written = false;
  }

  /**
   * Rewrites the header message. Should be called at the end of each round by any robot that uses messaging.
   * @throws GameActionException
   */
  public void endRound() throws GameActionException {
    if (message_written) {
      writeHeaderMessage(message_index);
      message_written = false;
    }
  }

  private void writeHeaderMessage(int total_messages) throws GameActionException {
    writeMessageAtIndex(HEADER_MESSAGE_INDEX, MessageType.HEADER.ordinal(), total_messages);
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

  /**
   * Announce robot's sequential SOLDIER_ID. Only called by HQ.
   * @param soldierID: soldier ID to assign.
   * @throws GameActionException
   */
  public void writeSoldierID(int soldierID) throws GameActionException {
    writeMessage(MessageType.SOLDIER_ID.ordinal(), soldierID);
  }
}
