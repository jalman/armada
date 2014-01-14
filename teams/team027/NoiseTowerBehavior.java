package team027;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;
import static team027.utils.Utils.*;

public class NoiseTowerBehavior extends RobotBehavior {
	

	int a=0, b=0; //for noise
	double[][] cows = null; double[] cowsindir = new double[8];
	Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static final int[] yrangefornoise = { 20, 19, 19, 19, 19, 19, 19, 18, 18, 17, 17, 16, 16, 15, 14, 13, 12, 10,
		8, 6, 0 };
	
	public NoiseTowerBehavior() {
	}

	/**
	 * Called at the beginning of each round.
	 */
  @Override
  public void beginRound() throws GameActionException {
  }

	/**
	 * Called every round.
	 */
  @Override
  public void run() throws GameActionException {
//		if(cows == null) {
//		cows = rc.senseCowGrowth();
//		for(int x = -20; x <= 20; x++) {
//			int range = yrangefornoise[Math.abs(x)];
//			for(int y = - range; y <= range; y++) {
//				cowsindir[Utils.getDirTowards(x,y)] += cows[curX+x][curY+y];
//			}
//		}
//	}
//	
	
	
//	if(a%2 == 0) {
		MapLocation target = RC.getLocation().add(directions[a %8], b);
		if(RC.canAttackSquare(target)) RC.attackSquare(target);
//		rc.yield();
//		target = target.add(directions[(a+3) %8]).add(directions[(a+2) %8], 2);
//		if(rc.canAttackSquare(target)) rc.attackSquare(target);
//		rc.yield();
//		target = target.add(directions[(a+6) %8], 6);
//		if(rc.canAttackSquare(target)) rc.attackSquare(target);
//	} else {
//		MapLocation target = rc.getLocation().add(directions[a %8], b);
//		if(rc.canAttackSquare(target)) rc.attackSquare(target);
//		rc.yield();
//		target = target.add(directions[(a+3) %8]).add(directions[(a+2) %8], 2);
//		if(rc.canAttackSquare(target)) rc.attackSquare(target);
//		rc.yield();
//		target = target.add(directions[(a+6) %8], 6);
//		if(rc.canAttackSquare(target)) rc.attackSquare(target);
//	}
	
	if(b>7) b--;
	else {
		a++;
		a%=8;
		int c = a%2 == 0 ? 20 : 14;
		for(b = 1; b <= c; b++) {
			MapLocation checkingplace = RC.getLocation().add(directions[a], b);
			TerrainTile check = RC.senseTerrainTile(checkingplace);
			if(check == TerrainTile.OFF_MAP || checkingplace.distanceSquaredTo(ENEMY_HQ) < 16) {
				b--;
				break;
			}
		}
	}
  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() {

  }
}
