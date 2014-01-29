package rookbot.nav;

public enum NavType {

  BUG(new BugMoveFun()),
  BUG_2(new BugMoveFun2()),
  HQ_DIJKSTRA(null);

  public final NavAlg navAlg;

  private NavType() {
    this.navAlg = null;
  }

  private NavType(NavAlg alg) {
    this.navAlg = alg;
  }
}
