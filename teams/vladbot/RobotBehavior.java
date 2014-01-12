package vladbot;

import battlecode.common.GameActionException;
import vladbot.messaging.MessageHandler;
import vladbot.messaging.MessagingSystem;

public abstract class RobotBehavior {
  protected MessageHandler[] handlers;

  public RobotBehavior() {
    handlers = new MessageHandler[MessagingSystem.MESSAGE_TYPES.length];
    initMessageHandlers();
  }

  /**
   * Override to specify message handlers.
   */
  protected void initMessageHandlers() {}

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
