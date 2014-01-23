package microbot3.nav;

public enum NavType {

  BUG_FUN(new BugMoveFun());

	public final NavAlg navAlg;

	private NavType() {
		this.navAlg = null;
	}

	private NavType(NavAlg alg) {
		this.navAlg = alg;
	}
}
