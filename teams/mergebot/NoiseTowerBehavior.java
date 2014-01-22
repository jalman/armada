package mergebot;

import mergebot.utils.Utils;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;
import static mergebot.utils.Utils.*;

public class NoiseTowerBehavior extends RobotBehavior {
	

	public static int a=6, b=0; //for noise
	double[][] cows = null; double[] cowsindir = new double[8];
	Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static final int[] yrangefornoise = { 17, 17, 17, 17, 16, 16, 16, 15, 15, 14, 14, 13, 12, 11, 10, 8, 6, 3 };
	
	public static MapLocation[][] paths = new MapLocation[8][30];
	public static int[] pathat = new int[8];
	
	public NoiseTowerBehavior() {
		for(int i = 7; i >= 0; i--) {
			int lastdir = (i+4) % 8;
			int at = 1;
			paths[i][0] = currentLocation;
			int lastcow = 0;
			for(int j = 1; j < 30; j++) {
				if(i < 7 && RC.isActive()) {
					try {
						makeSomeNoise();
					} catch (GameActionException e1) {
						e1.printStackTrace();
					}
				}
				
				int k = lastdir + 2;
				k %= 8;
				int bestscore = -1;
				MapLocation bestplace = currentLocation;
				while (k != (lastdir + 6) % 8) {
					
					int score = Math.abs(i - k);
					if(score < 4) score = 8 - score;
					score *= score*score;
					MapLocation here = paths[i][j-1].add(directions[k]);
					double cows;
					try {
						cows = Utils.COW_GROWTH[here.x][here.y];
						if(cows > 0) lastcow = j;
						score =  here.distanceSquaredTo(currentLocation) > 300 || RC.senseTerrainTile(here) == TerrainTile.VOID ? -10 : cows == 0.0  ? 30 : score + (int)(cows*10000) + 30;
					} catch (Exception e) {
						score = -2;
					}
					
					if(score > bestscore) {
						bestscore = score;
						bestplace = here;
					}
					
					k++;
					if(k == 8) k = 0;
				}
				paths[i][j] = bestplace;
			}
			pathat[i] = lastcow;
			if(lastcow < 29) lastcow++;
		}
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
	  while (!RC.isActive()) {
		  RC.yield();
	  }
	  makeSomeNoise();
  }

	/**
	 * Called at the end of each round.
	 */
	@Override
  public void endRound() {

  }
	
	public static void makeSomeNoise() throws GameActionException { //assumes RC is active
		  
		  
		  if(b < 3) {
			  a++;
			  if(a == 8) a = 0;
			  b = pathat[a];
			  System.out.println(a + " " + b + " " + pathat[a]);
		  } else {
			  b--;
		  }
		  if(paths[a][b] != null)
			  RC.attackSquare(paths[a][b]);
		
	}
}
