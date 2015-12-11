package jp.ne.sakura.gaur24.twitterbot.zasshoku;

/**
 * ユーザーごとの経験値を保持するクラス
 */
public class ZasshokuUser {

	// Twitterのアカウント固有のユーザーID
	private long userID;

	// ファイル書き込み時に使用するScreenNameを保持
	private String screenName;

	// これまでに取得した総経験値
	private int totalExp;

	// 次のレベルアップに必要な総経験値
	private int nextLevelUpTotalExp;

	// 現在のレベル
	private int level;

	/**
	 * 経験値の上限。このくらいじゃないと計算時間がやばいのでチェックする。
	 */
	public static final int MAX_EXP = 10000000;

	/**
	 * レベルの上限。経験値<code>MAX_EXP</code>取得時になるレベルより小さい値を設定。
	 */
	public static final int MAX_LEVEL = 4000;

	/**
	 * 新規ユーザーを生成するコンストラクタ
	 * 
	 * @param userID
	 * @param screenName
	 */
	public ZasshokuUser(long userID, String screenName) {
		this.userID = userID;
		this.screenName = screenName;

		// 初期ステータス
		this.totalExp = 0;
		this.level = 1;

		// レベル2になるのに必要な総経験値を計算
		this.nextLevelUpTotalExp = calcNextLevelUpTotalExp(level);
	}

	/**
	 * 既存ユーザーを生成するコンストラクタ
	 * 
	 * @param userID
	 * @param screenName
	 * @param totalExp
	 */
	public ZasshokuUser(long userID, String screenName, int totalExp) {
		// まずは新規ユーザーとしてパラメータを設定
		this.userID = userID;
		this.screenName = screenName;
		this.totalExp = 0;
		this.level = 1;
		this.nextLevelUpTotalExp = calcNextLevelUpTotalExp(this.level);

		// レベルアップさせる
		this.gainExp(totalExp);
		this.nextLevelUpTotalExp = calcNextLevelUpTotalExp(this.level);
	}

	/**
	 * gainedExp分の経験値を取得する<br>
	 * gainedExpが<code>MAX_EXP</code>以上であれば、<code>MAX_EXP</code>を取得する
	 * 
	 * @param gainedExp
	 * @return レベルアップしていればtrueを返す
	 */
	public boolean gainExp(int gainedExp) {
		boolean isLevelUp = false;

		// 0以下ならなにもしない
		if (gainedExp <= 0) {
			return false;
		}

		// MAX_EXPを超える場合はMAX_EXPとする
		if (gainedExp > MAX_EXP || (totalExp + gainedExp) > MAX_EXP) {
			totalExp = MAX_EXP;
		} else {
			totalExp += gainedExp;
		}

		// レベルアップ処理
		while (totalExp >= nextLevelUpTotalExp) {
			// レベルは上限を超えない
			if (level >= MAX_LEVEL) {
				break;
			}
			level++;
			nextLevelUpTotalExp = calcNextLevelUpTotalExp(level);
			isLevelUp = true;

			// オーバーフロー対策
			if (nextLevelUpTotalExp < 0) {
				break;
			}
		}
		return isLevelUp;
	}

	/**
	 * レベルアップするために必要な総経験値を計算して返す<br>
	 * 
	 * @param currentLevel
	 * @return nextLevelUpExp
	 */
	private static int calcNextLevelUpTotalExp(int currentLevel) {
		if (currentLevel > MAX_LEVEL) {
			return -1;
		}
		int returnExp = 0;
		// レベル高いユーザーの場合、計算量が多い
		// Integer.MAXとか入れると終わらないので注意
		for (int i = 1; i <= currentLevel; i++) {
			// 1.1 * 現在のレベル の切り上げの総和
			returnExp += Math.ceil(1.1d * (double) i);
		}
		return returnExp;
	}

	public int getNextLevelUpExp() {
		return nextLevelUpTotalExp - totalExp;
	}

	public long getUserID() {
		return userID;
	}

	public String getScreenName() {
		return screenName;
	}

	public int getTotalExp() {
		return totalExp;
	}

	public int getNextLevelUpTotalExp() {
		return nextLevelUpTotalExp;
	}

	public int getLevel() {
		return level;
	}
	
	/**
	 * ファイル出力用にユーザー情報を出力
	 */
	@Override
	public String toString() {
		return screenName + "," + totalExp;
	}

}
