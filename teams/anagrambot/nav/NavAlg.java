package anagrambot.nav;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public abstract class NavAlg {
  public static boolean AVOID_ENEMY_HQ = true;

  protected MapLocation curLoc = null, finish = null;

  abstract public void recompute();

  abstract public void recompute(MapLocation finish);

  /*
   * Return the next direction to [attempt to] move in.
   * 
   */
  abstract public Direction getNextDir();
}
