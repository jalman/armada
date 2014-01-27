package pogbot.noise;

import pogbot.RobotBehavior;
import pogbot.utils.Utils;
import battlecode.common.*;
import static pogbot.utils.Utils.*;

public class SpiralNoiseTowerBehavior extends BFSNoiseTower {
	
	 public SpiralNoiseTowerBehavior() {
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
  
  
  int i = 3;
	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {
    if(!RC.isActive()) return;
    i--;
    if(queue[i].distanceSquaredTo(currentLocation) < 5) {
      i = at-1;
    }
    RC.attackSquare(queue[i]);
  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() throws GameActionException {
	  messagingSystem.endRound();
  }
	
}
