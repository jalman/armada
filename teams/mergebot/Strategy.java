package mergebot;

import java.util.*;

public class Strategy {

  public enum GamePhase {
    OPENING,
    MIDGAME,
    ENDGAME;
  }

  public int desiredPASTRNum; // desired # PASTRs
  public int[] PASTRThresholds; // # index i: bots required to build (i+1)st PASTR
  // public int secondPASTRThreshold; // # bots required to build second PASTR
  public boolean aggressive; // whether to be aggressive or not (maybe change to int and use in
                             // deciding to defend/attack and/or in micro?)

  public Strategy(int desiredPASTRNum, int PASTRThreshold, int secondPASTRThreshold,
      boolean aggressive) {
    this.desiredPASTRNum = desiredPASTRNum;
    this.PASTRThresholds = new int[] {PASTRThreshold, secondPASTRThreshold, 1000, 1000};
    this.aggressive = aggressive;
  }

  public static final Strategy INIT_DOUBLE_PASTR = new Strategy(2, 0, 2, true);
  public static final Strategy INIT_EARLY_SINGLE_PASTR = new Strategy(1, 3, 100, true);
  public static final Strategy INIT_SINGLE_PASTR = new Strategy(1, 5, 100, true);
  public static final Strategy INIT_LATE_SINGLE_PASTR = new Strategy(1, 7, 100, true);
  public static final Strategy INIT_RUSH = new Strategy(1, 11, 100, true);

  public static final Strategy MID_DOUBLE_PASTR_AGGRESSIVE = new Strategy(2, 5, 12, true);
  public static final Strategy MID_SINGLE_PASTR_AGGRESSIVE = new Strategy(1, 5, 100, true);

  /**
   * Since we'll mostly be using the constants defined above, we can usually use == instead of .equals
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj instanceof Strategy) {
      Strategy s = (Strategy) obj;
      return (this.desiredPASTRNum == s.desiredPASTRNum
          && Arrays.equals(this.PASTRThresholds, s.PASTRThresholds) && this.aggressive == s.aggressive);
    }
    return false;
  }

  @Override
  public String toString() {
    return "p: " + this.desiredPASTRNum + ", 1: " + this.PASTRThresholds[0] + ", 2: "
        + this.PASTRThresholds[1] + ", a: " + this.aggressive;
  }
}