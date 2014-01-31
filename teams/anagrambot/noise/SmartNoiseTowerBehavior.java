package anagrambot.noise;

import anagrambot.RobotBehavior;
import anagrambot.utils.Utils;
import battlecode.common.*;
import static anagrambot.utils.Utils.*;

public class SmartNoiseTowerBehavior extends BFSNoiseTower {
	
  int[] places = new int[8];
  boolean[] skip = new boolean[8];
  
	 public SmartNoiseTowerBehavior() throws GameActionException {
	   super();
	   
	   for(int i = at-1; i > 0; i--) {
	     MapLocation loc = queue[i];
	     int dir = currentLocation.directionTo(loc).ordinal();
	     if(COW_GROWTH[loc.x][loc.y] > 0 && currentLocation.distanceSquaredTo(loc) > currentLocation.distanceSquaredTo(queue[places[dir]])) {
	       places[dir] = i;
	     }
	   }
	   
	   
	   System.out.println("SKIP" + Clock.getBytecodeNum());
	   for(int i = 7; i >= 0; i--) for(int j = i-1; j >= 0; j--) {
	     if(skip[i] || skip[j]) continue;
	     MapLocation ml = queue[places[i]];
	     MapLocation comp = queue[places[j]];
	     while(ml.distanceSquaredTo(currentLocation) > 5) {
	       if(ml.distanceSquaredTo(comp) <= 4) {
	         skip[j] = true;
	         break;
	       }
	       ml = ml.add(dir[ml.x - currentLocation.x + 17][ml.y - currentLocation.y + 17]);
	     }
	   }
     System.out.println("SKIP" + Clock.getBytecodeNum());
	   
	   for(int i = 7; i >= 0; i--) {
	     System.out.println(i + " " + queue[places[i]] + " " + skip[i]);
	     if(!skip[i]) {
	       target = queue[places[i]];
	       //break;
	     }
	   }
	   
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
	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {
    if(!RC.isActive()) return;
    
    inner: {
      if(target.distanceSquaredTo(currentLocation) < 3) {
        a++;
        if(a%2 == 1) {
          if(!nearbyCows()) {
            a++;
          } else {
            break inner;
          }
        }
        while(a < 16 && skip[a/2]) a+=2;
        
        if(a>=16) {
          if(!nearbyCows()) {
            a = 0;
            target = queue[places[a/2]];
          }
        } else {
          target = queue[places[a/2]];
        }
        
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
    if(a > 24) return false;
    
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
    
    if(numCows > (a<16 ? 1500 : 600)) {
      RC.setIndicatorString(2, "nearby at " + target + " on turn "+ Clock.getRoundNum());
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
