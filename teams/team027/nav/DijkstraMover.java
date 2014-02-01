package team027.nav;

import static team027.utils.Utils.*;
import team027.utils.*;
import battlecode.common.*;

/**
 * Moves towards the DIJKSTRA_CENTER (that is, the ALLY_HQ).
 * @author vlad
 *
 */
public class DijkstraMover extends GradientMover {
  private static DijkstraMover dijkstraMover;

  public static DijkstraMover getDijkstraMover() {
    if (dijkstraMover == null) {
      dijkstraMover = new DijkstraMover();
    }
    return dijkstraMover;
  }

  @Override
  public int getWeight(MapLocation loc) {
    try {
      Pair<Direction, Integer> pathingInfo = messagingSystem.readPathingInfo(loc);
      if (pathingInfo.first != null) {
        return pathingInfo.second;
      }
    } catch (GameActionException e) {
      // TODO Auto-generated catch block
      // e.printStackTrace();
    }
    return Integer.MAX_VALUE;
  }

}
