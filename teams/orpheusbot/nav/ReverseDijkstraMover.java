package orpheusbot.nav;

import static orpheusbot.utils.Utils.*;
import orpheusbot.utils.Pair;
import battlecode.common.*;

/**
 * Moves towards the reflection of the DIJKSTRA_CENTER (that is, the ENEMY_HQ).
 * @author vlad
 *
 */
public class ReverseDijkstraMover extends GradientMover {
  public static final ReverseDijkstraMover reverseDijkstraMover = new ReverseDijkstraMover();

  @Override
  public int getWeight(MapLocation loc) {
    try {
      Pair<Direction, Integer> pathingInfo =
          messagingSystem.readPathingInfo(getSymmetricSquare(loc));
      if (pathingInfo.first != null) {
        return pathingInfo.second;
      }
    } catch (GameActionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return Integer.MAX_VALUE;
  }

}
