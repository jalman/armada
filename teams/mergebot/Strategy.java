package mergebot;


public class Strategy {

  public enum GamePhase {
    OPENING,
    MIDGAME,
    ENDGAME;
  }

  public enum InitialStrategy {
    DOUBLE_PASTR(0, 2, 2), // immediately build 2 pastrs
    EARLY_SINGLE_PASTR(3, 100, 1), // build pastr after 3
    SINGLE_PASTR(5, 100, 1), // build pastr after 5
    LATE_SINGLE_PASTR(7, 100, 1), // build pastr after 7
    RUSH(11, 100, 1), // build pastr after 11
    ;

    public int PASTRThreshold, secondPASTRThreshold;
    public int desiredPASTRNum;

    private InitialStrategy(int firstThreshold, int secondThreshold, int PASTRNum) {
      this.PASTRThreshold = firstThreshold;
      this.secondPASTRThreshold = secondThreshold;
      this.desiredPASTRNum = PASTRNum;
    }
  }

  public enum MidgameStrategy {
    DOUBLE_PASTR_AGGRESSIVE(2, true),
    // DOUBLE_PASTR_PASSIVE,
    SINGLE_PASTR_AGGRESSIVE(1, true),
    SINGLE_PASTR_PASSIVE(1, false), ;

    public int desiredPASTRNum;
    public boolean aggressive;

    private MidgameStrategy(int desiredPASTRNum, boolean aggressive) {
      this.desiredPASTRNum = desiredPASTRNum;
      this.aggressive = aggressive;
    }
  }
}