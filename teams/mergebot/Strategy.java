package mergebot;

public class Strategy {

  public enum GamePhase {
    OPENING,
    MIDGAME,
    ENDGAME;
  }

  public int desiredPASTRNum; // desired # PASTRs
  public int PASTRThreshold; // # bots required to build a PASTR
  public int secondPASTRThreshold; // # bots required to build second PASTR
  public boolean aggressive; // whether to be aggressive or not (maybe change to int and use in
                             // deciding to defend/attack and/or in micro?)

  public Strategy(int desiredPASTRNum, int PASTRThreshold, int secondPASTRThreshold,
      boolean aggressive) {
    this.desiredPASTRNum = desiredPASTRNum;
    this.PASTRThreshold = PASTRThreshold;
    this.secondPASTRThreshold = secondPASTRThreshold;
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
      return (this.desiredPASTRNum == s.desiredPASTRNum && this.PASTRThreshold == s.PASTRThreshold
          && this.secondPASTRThreshold == s.secondPASTRThreshold && this.aggressive == s.aggressive);
    }
    return false;
  }

  @Override
  public String toString() {
    return "p: " + this.desiredPASTRNum + ", 1: " + this.PASTRThreshold + ", 2: "
        + this.secondPASTRThreshold + ", a: " + this.aggressive;
  }
}