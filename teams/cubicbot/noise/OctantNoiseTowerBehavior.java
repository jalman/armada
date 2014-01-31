package cubicbot.noise;

import cubicbot.RobotBehavior;
import cubicbot.utils.Utils;
import battlecode.common.*;
import static cubicbot.utils.Utils.*;

public class OctantNoiseTowerBehavior extends BFSNoiseTower {
	
  int[] places = new int[8];
  boolean hasbeenPASTR = false;
  
	 public OctantNoiseTowerBehavior() throws GameActionException {
	   super();
	   
	   for(int i = at-1; i > 0; i--) {
	     MapLocation loc = queue[i];
	     int dir = currentLocation.directionTo(loc).ordinal();
	     if(COW_GROWTH[loc.x][loc.y] > 0 && currentLocation.distanceSquaredTo(loc) > currentLocation.distanceSquaredTo(queue[places[dir]])) {
	       places[dir] = i;
	     }
	   }
	   
	   target = queue[places[0]];
	 }
	
	/**
	 * Called at the beginning of each round.
	 */
  @Override
  public void beginRound() throws GameActionException {
    Utils.updateBuildingUtils();
    messagingSystem.beginRound(handlers);
  }
  
  
  int a = 0;
  MapLocation target;
  
  public boolean isNearbyPASTR() {
    for (MapLocation m : ALLY_PASTR_LOCS) {
      if(m.distanceSquaredTo(currentLocation) <= 2) return true;
    }
    return false;
  }
  
	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {
    if(!RC.isActive()) return;
    
    if(isNearbyPASTR()) {
      hasbeenPASTR = true;
    } else if(hasbeenPASTR) {
      for(MapLocation m : ENEMY_PASTR_LOCS) {
        if(RC.canAttackSquare(m)) {
          RC.attackSquare(m);
          return;
        }
      }
    }
    
    if(target.distanceSquaredTo(currentLocation) < 3) {
      a++;
      if(a>=8) {
        if(!nearbyCows()) {
          a = 0;
          target = queue[places[a]];
        }
      } else {
        target = queue[places[a]];
      }
      
    }

    int x = target.x - currentLocation.x + 17;
    int y = target.y - currentLocation.y + 17;
    
    
    MapLocation realTarget = dir[x][y] != null ? target.add(dir[x][y].opposite(), 3) : target;
    
    
    
    target = dir[x][y] != null ? target.add(dir[x][y]) : currentLocation;
    
    if(RC.canAttackSquare(realTarget)) RC.attackSquare(realTarget);
    else run();

    
//    int x = target.x - currentLocation.x + 17;
//    int y = target.y - currentLocation.y + 17;
//    
//    MapLocation target2 = target.add(dir[x][y]);
//    
//    x = target2.x - currentLocation.x + 17;
//    y = target2.y - currentLocation.y + 17;
//    
//    MapLocation target3 = target2.add(dir[x][y]);
//
//    if(target3.directionTo(target2).equals(target2.directionTo(target))) {
//      target = target3;
//    } else {
//      target = target2;
//    }
  }
  
  
  public boolean nearbyCows() throws GameActionException {
    if(a > 16) return false;
    
    MapLocation best = null;
    double numCows = -1.0;
    for(int x = -8; x <= 8; x++) {
      for(int y = -8; y <= 8; y++) {
        MapLocation lookat = currentLocation.add(x, y);
        if(lookat.distanceSquaredTo(ALLY_PASTR_COUNT > 0 ? ALLY_PASTR_LOCS[0] : currentLocation) <= 5 || !RC.canSenseSquare(lookat)) continue;
        double t = RC.senseCowsAtLocation(lookat);
        if(t > numCows) {
          numCows = t;
          best = lookat;
        }
      }
    }
    
    if(numCows > 300) {
      target = best;
      //System.out.println(target + " " + numCows);
      return true;
    }
    return false;
  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() throws GameActionException {
	  messagingSystem.endRound();
	  RC.setIndicatorString(0, "the target is " + target + "");
    RC.setIndicatorString(1, "a is " + a);
  }
	
}
