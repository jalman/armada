package vladbot;

import static vladbot.utils.Utils.*;
import vladbot.messaging.MessageHandler;
import vladbot.messaging.MessagingSystem;
import battlecode.common.GameActionException;

public abstract class RobotBehavior {
  protected MessageHandler[] handlers;

  public RobotBehavior() {
    handlers = new MessageHandler[MessagingSystem.MESSAGE_TYPES.length];
    initMessageHandlers();
  }

  /**
   * Override to specify message handlers.
   */
  protected void initMessageHandlers() {
    handlers[MessagingSystem.MessageType.MILK_INFO.type] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        ALLY_PASTR_COUNT = message[0];
        ENEMY_PASTR_COUNT = message[1];
        ALLY_MILK = message[2];
        ENEMY_MILK = message[3];
      }
    };
  }

  /**
   * Called at the beginning of each round.
   * @return whether it's worth living to the next round.
   */
  public abstract void beginRound() throws GameActionException;

  /**
   * Called every round.
   */
  public abstract void run() throws GameActionException;

  /**
   * Called at the end of each round.
   * @throws GameActionException
   */
  public abstract void endRound() throws GameActionException;
}
