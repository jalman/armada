package vladbot.hq;

import static vladbot.utils.Utils.*;
import vladbot.RobotBehavior;
import vladbot.messaging.MessageHandler;
import vladbot.messaging.MessagingSystem.MessageType;
import vladbot.utils.Utils;
import battlecode.common.*;

public class HQBehavior extends RobotBehavior {

  // HQAction[] buildOrder;
  // int buildOrderProgress = 0;

  int numBots, numNoiseTowers, numPastrs, numSoldiers;
  int SoldierID = 0;

  private final AttackSystem attackSystem = new AttackSystem();

  public static final int[] yrangefornoise = { 17, 17, 17, 17, 16, 16, 16, 15, 15, 14, 14, 13, 12, 11, 10, 8, 6, 3 };

  public HQBehavior() {

    //pick a strategy
    double totalcows = 0.0;
    for(int x = Math.max(-17, -curX); x <= Math.min(17, MAP_WIDTH - 1 - curX); x++) {
      int range = yrangefornoise[Math.abs(x)];
      for(int y = Math.max(- range, -curY); y <= Math.min(range, MAP_HEIGHT - 1 - curY); y++) {
        totalcows += COW_GROWTH[curX+x][curY+y];
      }
    }
  }


  @Override
  protected void initMessageHandlers() {
    handlers[MessageType.ATTACK_LOCATION.ordinal()] = new MessageHandler() {
      @Override
      public void handleMessage(int[] message) {
        // MapLocation loc = new MapLocation(message[0], message[1]);
        // TODO: attack!
      }
    };
  }

  @Override
  public void beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    // RC.setIndicatorString(0, generators.size + " generators. " + Double.toString(actualFlux) +
    // " is pow");
    numBots = RC.senseNearbyGameObjects(Robot.class, currentLocation, 10000, ALLY_TEAM).length;
    messagingSystem.beginRound(handlers);
  }

  @Override
  public void run() throws GameActionException {
    macro();
    attackSystem.tryAttack();
  }

  @Override
  public void endRound() throws GameActionException {
    messagingSystem.endRound();
  }

  /**
   * Handle upgrades and robots.
   */
  private void macro() {
    try {
      buildSoldier();
    } catch (GameActionException e) {
      e.printStackTrace();
    }
  }

  /**
   * Tries to build a Soldier.
   * @return Whether successful.
   * @throws GameActionException
   */
  boolean buildSoldier() throws GameActionException {
    return buildSoldier(ALLY_HQ.directionTo(ENEMY_HQ));
  }

  /**
   * Tries to build a Soldier.
   * @param dir The direction in which to build.
   * @return Whether successful.
   * @throws GameActionException
   */
  boolean buildSoldier(Direction dir) throws GameActionException {
    if (RC.isActive() && RC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
      // Spawn soldier
      for (int i = 0; i < 8; i++) {
        // if square is movable, spawn soldier there and send initial messages
        if (RC.canMove(dir)) {
          sendMessagesOnBuild();
          RC.spawn(dir);
          return true;
        }
        // otherwise keep rotating until this is possible
        dir = dir.rotateRight();
      }
      // message guys to get out of the way??
    }
    return false;
  }

  private boolean goodPlaceToMakeSoldier(Direction dir) {
    return RC.canMove(dir);
  }

  private void sendMessagesOnBuild() throws GameActionException {
    // empty for now
  }
}
