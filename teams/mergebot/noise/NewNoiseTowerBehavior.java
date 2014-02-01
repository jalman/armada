package mergebot.noise;

import static mergebot.utils.Utils.*;
import mergebot.utils.*;
import battlecode.common.*;

public class NewNoiseTowerBehavior extends BFSNoiseTower {

	 public NewNoiseTowerBehavior() throws GameActionException {
	   super();
	 }

	/**
	 * Called at the beginning of each round.
	 */
  @Override
  public void beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    messagingSystem.beginRound(handlers);
  }

  public void pickNewTarget() {
    double bestEstimate = -1.0;
    MapLocation bestPlace = null;
    for(int i = at-1; i > 0; i--) {
      MapLocation loc = queue[i];
      if(loc.distanceSquaredTo(currentLocation) < 30) continue;
      int x = loc.x - currentLocation.x + 17;
      int y = loc.y - currentLocation.y + 17;
      double estimate = COW_GROWTH[loc.x][loc.y]*(Clock.getRoundNum() - last[x][y]);
      int d = loc.distanceSquaredTo(currentLocation);
      estimate += d*d*d;
      if(estimate > bestEstimate) {
        bestEstimate = estimate;
        bestPlace = loc;
      }
    }
    int x = bestPlace.x - currentLocation.x + 17;
    int y = bestPlace.y - currentLocation.y + 17;
    // System.out.println(bestPlace + " " + x + " " + y + " " + bestEstimate);
    target = bestPlace;

  }

  public void advanceTarget() {
    if(target == null || target.distanceSquaredTo(currentLocation) < 5) pickNewTarget();

    target = target.add(dir[target.x - currentLocation.x + 17][target.y - currentLocation.y + 17]);
  }

  public boolean fireAtTarget() throws GameActionException {
    int x = target.x - currentLocation.x + 17;
    int y = target.y - currentLocation.y + 17;
    Direction d = dir[x][y];
    MapLocation fireAt = target.add(d.opposite());
    if(RC.canAttackSquare(fireAt)) {
      RC.attackSquare(fireAt);
      for(int i = Math.max(fireAt.x - 2, currentLocation.x - 17); i <= Math.min(fireAt.x + 2, currentLocation.x + 17); i++)  for(int j = Math.max(fireAt.y - 2, currentLocation.y - 17); j <= Math.min(fireAt.y + 2, currentLocation.y + 17); j++) {
        last[i - currentLocation.x + 17][j - currentLocation.y + 17] = Clock.getRoundNum();
          // System.out.println(fireAt + " " + (i - currentLocation.x + 17) + " " + (j -
          // currentLocation.y + 17));
      }

      return true;
    }
    return false;
  }

	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {
    advanceTarget();
    while(target == null) pickNewTarget();
    if(RC.isActive()) {
      while(!fireAtTarget()) {
        advanceTarget();
      }
    }
  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() throws GameActionException {
	  messagingSystem.endRound();
  }

}
