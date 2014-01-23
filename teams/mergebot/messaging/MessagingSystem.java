package mergebot.messaging;

import static mergebot.utils.Utils.*;
import battlecode.common.*;

/**
 * Efficiently send and receive messages.
 * @author vlad
 */
public class MessagingSystem {

  /**
   * The types of messages that can be sent.
   * @author vlad
   */
  public enum MessageType {

    PARAMETERS(2),
    ATTACK_LOCATION(3),
    MICRO_INFO(3),
    BIRTH_INFO(4),
    SOLDIER_ID(1),
    ENEMY_BOT(2),
    MILK_INFO(4),
    BUILD_PASTURE(2),
    BUILDING_PASTURE(2);
    ;

    public final int type = this.ordinal();

    /**
     * Number of integers that comprise this message.
     */
    public final int length;

    private MessageType(int length) {
      this.length = length;
    }
  }

  public static final MessageType[] MESSAGE_TYPES = MessageType.values();

  /**
   * Needs to be larger than any message size.
   */
  public static final int MESSAGE_SIZE = 6;
  public static final int BLOCK_SIZE = 1 + MESSAGE_SIZE;

  private static final int RESERVED_CHANNELS = 5000;
  private static final int MESSAGE_CHANNELS = GameConstants.BROADCAST_MAX_CHANNELS
      - RESERVED_CHANNELS;
  private static final int MAX_MESSAGE_INDEX = MESSAGE_CHANNELS / BLOCK_SIZE;

  /**
   * Enumerates the reserved message channels.
   * If you want to reserve N channels, add an entry of length N.
   * Then use read/writeReservedMessage to use the channel.
   * @author vlad
   */
  public enum ReservedMessageType {
    MESSAGE_INDEX(1),
    JOSHBOT(1);

    public final int type = this.ordinal();
    public final int length;

    private ReservedMessageType(int length) {
      this.length = length;
    }

    public int channel() {
      return reserved_channel_indices[this.type];
    }
  }

  public static int[] reserved_channel_indices = new int[ReservedMessageType.values().length];

  public static final double BROADCAST_COST = 10;

  public static final int HQ = RobotType.HQ.ordinal(),
      SOLDIER = RobotType.SOLDIER.ordinal();

  /**
   * The total number of messages posted by our team.
   */
  public int message_index;

  /**
   * Whether any messages were written this round.
   */
  private boolean message_written;

  private boolean first_round = true;

  public MessagingSystem() {
    initReservedChannels();
  }

  /**
   * Initialize the reserved_channel_indices array, necessary for correct function of reserved channels
   */
  public static void initReservedChannels() {
    int currentChannel = MESSAGE_CHANNELS; // start of reserved channels is at MESSAGE_CHANNELS
    ReservedMessageType[] rmt = ReservedMessageType.values();
    for (int i = 0; i < rmt.length; i++) {
      reserved_channel_indices[i] = currentChannel;
      currentChannel += rmt[i].length;
    }
  }

  /**
   * Reads a reserved message at rm.channel().
   * @param rm A Reserved MessageType
   * @throws GameActionException
   */
  public int readReservedMessage(ReservedMessageType rm) throws GameActionException {
    return RC.readBroadcast(rm.channel());
  }

  /**
   * Reads a reserved message at rm.channel() + offset; DOES NOT CHECK IF THIS GOES OUT OF THE ALLOTTED
   * CHANNELS FOR ReservedMessageType rm!!!!
   * @param rm A Reserved MessageType
   * @param offset Channel offset to read broadcast at
   * @throws GameActionException
   */
  public int readReservedMessage(ReservedMessageType rm, int offset) throws GameActionException {
    return RC.readBroadcast(rm.channel() + offset);
  }

  /**
   * Writes a reserved message at rm.channel().
   * @param rm A Reserved MessageType
   * @param message Message
   * @throws GameActionException
   */
  public void writeReservedMessage(ReservedMessageType rm, int message) throws GameActionException {
    RC.broadcast(rm.channel(), message);
  }

  /**
   * Writes a reserved message at rm.channel() + offset; DOES NOT CHECK IF THIS GOES OUT OF THE ALLOTTED
   * CHANNELS FOR ReservedMessageType rm!!!!
   * @param rm A Reserved MessageType
   * @param offset Channel offset to broadcast at
   * @param message Message
   * @throws GameActionException
   */
  public void writeReservedMessage(ReservedMessageType rm, int offset, int message)
      throws GameActionException {
    RC.broadcast(rm.channel() + offset, message);
  }


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
    int new_index = readReservedMessage(ReservedMessageType.MESSAGE_INDEX);

    int[] buffer = new int[MESSAGE_SIZE];

    for (int index = message_index; index < new_index; index++) {
      int type = readMessage(index, buffer);
      if (handlers[type] != null) {
        handlers[type].handleMessage(buffer);
      } else {
        // print error message?
        // System.out.println("ERROR?: missing message handler for type " + MESSAGE_TYPES[type]
        // + " at index " + index);
      }
    }

    message_index = new_index;
  }

  /**
   * Writes a message to the global radio.
   * @param type The type of message.
   * @param message The message data.
   * @throws GameActionException
   */
  public void writeMessage(MessageType type, int... message) throws GameActionException {
    int channel = (message_index++ % MAX_MESSAGE_INDEX) * BLOCK_SIZE;

    RC.broadcast(channel++, type.type);

    for (int i = 0; i < message.length; i++) {
      RC.broadcast(channel++, message[i]);
    }

    message_written = true;
  }

  public void beginRound(MessageHandler[] handlers) throws GameActionException {
    if (!first_round) {
      readMessages(handlers);
      message_written = false;
    } else {
      message_index = readReservedMessage(ReservedMessageType.MESSAGE_INDEX);
      first_round = false;
    }
  }

  /**
   * Rewrites the header message. Should be called at the end of each round by any robot that uses messaging.
   * @throws GameActionException
   */
  public void endRound() throws GameActionException {
    if (message_written) {
      RC.broadcast(ReservedMessageType.MESSAGE_INDEX.channel(), message_index);
      message_written = false;
    }
  }

  /**
   * Announce somewhere to attack.
   * @param loc: place to attack
   * @param priority: priority of attack
   * @throws GameActionException
   */
  public void writeAttackMessage(MapLocation loc) throws GameActionException {
    // writeMessage(MessageType.ATTACK_LOCATION.type, loc.x, loc.y);
    int channel = (message_index++ % MAX_MESSAGE_INDEX) * BLOCK_SIZE;
    RC.broadcast(channel++, MessageType.ATTACK_LOCATION.type);
    RC.broadcast(channel++, loc.x);
    RC.broadcast(channel, loc.y);
    message_written = true;
  }

  /**
   * Announce sighting of enemy bot.
   * @param loc
   * @throws GameActionException
   */
  public void writeEnemyBotMessage(MapLocation loc) throws GameActionException {
    int channel = (message_index++ % MAX_MESSAGE_INDEX) * BLOCK_SIZE;
    RC.broadcast(channel++, MessageType.ENEMY_BOT.type);
    RC.broadcast(channel++, loc.x);
    RC.broadcast(channel, loc.y);
    message_written = true;
  }

  /**
   * Announce somewhere to micro.
   * @param loc
   * @param goIn
   * @throws GameActionException
   */
  public void writeMicroMessage(MapLocation loc, int goIn) throws GameActionException {
    writeMessage(MessageType.MICRO_INFO, loc.x, loc.y, goIn);
  }

  /**
   * Announce a robot's birth. (Usually an encampment.)
   * @param loc: location of birth
   * @param id: id of new robot
   * @param type: type of new robot
   * @throws GameActionException
   */
  public void writeBirthMessage(MapLocation loc, int id, int type) throws GameActionException {
    writeMessage(MessageType.BIRTH_INFO, loc.x, loc.y, id, type);
  }

  /**
   * Announce robot's sequential SOLDIER_ID. Only called by HQ.
   * @param soldierID: soldier ID to assign.
   * @throws GameActionException
   */
  public void writeSoldierID(int soldierID) throws GameActionException {
    writeMessage(MessageType.SOLDIER_ID, soldierID);
  }

  /**
   * Used by the HQ to save soldier bytecodes.
   * @param allyPastrCount
   * @param enemyPastrCount
   * @param allyMilk
   * @param enemyMilk
   * @throws GameActionException
   */
  public void writeMilkInfo(int allyPastrCount, int enemyPastrCount, int allyMilk, int enemyMilk)
      throws GameActionException {
    writeMessage(MessageType.MILK_INFO, allyPastrCount, enemyPastrCount, allyMilk, enemyMilk);
  }

  public void writeBuildPastureMessage(MapLocation loc) throws GameActionException {
    writeMessage(MessageType.BUILD_PASTURE, loc.x, loc.y);
  }

  public void writeBuildingPastureMessage(MapLocation target) throws GameActionException {
    writeMessage(MessageType.BUILDING_PASTURE, target.x, target.y);
  }
}
