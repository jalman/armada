package examplejurgzplayer;

import battlecode.common.GameActionException;
import examplejurgzplayer.messaging.MessageHandler;
import examplejurgzplayer.messaging.MessageType;

public abstract class RobotBehavior {
  protected MessageHandler[] handlers;

  public RobotBehavior() {
    initMessageHandlers();
  }

  /**
   * Override to specify message handlers.
   */
  protected void initMessageHandlers() {
    handlers = new MessageHandler[MessageType.MESSAGE_TYPES.length];
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
