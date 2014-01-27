package mergebot.noise;

import mergebot.RobotBehavior;
import mergebot.utils.Utils;
import battlecode.common.*;
import static mergebot.utils.Utils.*;

public class OctantNoiseTowerBehavior extends BFSNoiseTower {
	
  int[] places = new int[8];
  
	 public OctantNoiseTowerBehavior() {
	   super();
	   
	   for(int i = at-1; i > 0; i--) {
	     int dir = currentLocation.directionTo(queue[i]).ordinal();
	     if(currentLocation.distanceSquaredTo(queue[i]) > currentLocation.distanceSquaredTo(queue[places[dir]])) {
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
	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {
    if(!RC.isActive()) return;
    
    if(target.distanceSquaredTo(currentLocation) < 3) {
      a++;
      if(a==8) a = 0;
      target = queue[places[a]];
    }
    
    MapLocation realTarget = target.add(currentLocation.directionTo(target), 3);
    
    if(RC.canAttackSquare(realTarget)) RC.attackSquare(realTarget);
    
    int x = target.x - currentLocation.x + 17;
    int y = target.y - currentLocation.y + 17;
    
    target = target.add(dir[x][y]);

  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() throws GameActionException {
	  messagingSystem.endRound();
  }
	
}
