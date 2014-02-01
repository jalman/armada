package mergebot.nav;

import static mergebot.utils.Utils.*;
import mergebot.utils.Pair;
import battlecode.common.*;

/**
 * Moves towards the DIJKSTRA_CENTER (that is, the ALLY_HQ).
 * @author vlad
 *
 */
public class DijkstraMover extends GradientMover {
  public static final DijkstraMover dijkstraMover = new DijkstraMover();

  @Override
  public int getWeight(MapLocation loc) {
    try {
      Pair<Direction, Integer> pathingInfo = messagingSystem.readPathingInfo(loc);
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
