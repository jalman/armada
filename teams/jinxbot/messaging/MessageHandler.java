package jinxbot.messaging;

import battlecode.common.GameActionException;

public interface MessageHandler {
  public void handleMessage(int[] message) throws GameActionException;
}
