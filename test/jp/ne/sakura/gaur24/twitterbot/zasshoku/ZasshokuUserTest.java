package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ZasshokuUserTest {

	/**
	 * 新規ユーザー登録時に呼び出すコンストラクタのテスト
	 */
	@Test
	public void newZasshokuUserTest() {
		long userID = 1L;
		String screenName = "screenName";
		ZasshokuUser user = new ZasshokuUser(userID, screenName);

		// userID
		assertEquals(userID, user.getUserID());

		// screenName
		assertEquals(screenName, user.getScreenName());

		// 初期レベルは1
		assertEquals(1, user.getLevel());

		// 初期総経験値は0
		assertEquals(0, user.getTotalExp());

		// レベル2になるために必要な経験値は総経験値2
		assertEquals(2, user.getNextLevelUpTotalExp());

		// レベル2になるために必要な経験値は2
		assertEquals(2, user.getNextLevelUpExp());
	}

	/**
	 * 既存ユーザー登録時に呼び出すコンストラクタのテスト 経験値0とした場合、新規ユーザーのコンストラクタと同じであることを確認
	 */
	@Test
	public void existingZasshokuUserTest() {
		long userID = 1L;
		String screenName = "screenName";
		int totalExp = 0;
		ZasshokuUser user = new ZasshokuUser(userID, screenName, totalExp);
		ZasshokuUser newUser = new ZasshokuUser(userID, screenName);

		// userID
		assertEquals(newUser.getUserID(), user.getUserID());

		// screenName
		assertEquals(newUser.getScreenName(), user.getScreenName());

		// 初期レベルは1
		assertEquals(newUser.getLevel(), user.getLevel());

		// 初期総経験値は0
		assertEquals(newUser.getTotalExp(), user.getTotalExp());

		// レベル2になるために必要な経験値は総経験値2
		assertEquals(newUser.getNextLevelUpTotalExp(), user.getNextLevelUpTotalExp());

		// レベル2になるために必要な経験値は2
		assertEquals(newUser.getNextLevelUpExp(), user.getNextLevelUpExp());
	}

	/**
	 * レベルアップのテスト
	 */
	@Test
	public void levelUpTest() {
		long userID = 1L;
		String screenName = "screenName";
		ZasshokuUser user = new ZasshokuUser(userID, screenName);

		for (int i = 0; i < 100; i++) {

			// レベルのテスト
			assertEquals(i + 1, user.getLevel());

			// レベルアップまで経験値を取得し続ける
			user.gainExp(user.getNextLevelUpExp());
		}

		user = new ZasshokuUser(userID, screenName);
		for (int i = 0; i < 100; i++) {
			assertEquals(i + 1, user.getLevel());
			// レベルアップまで1ずつ経験値を取得しても同じ結果なのをテスト
			while (!user.gainExp(1)) {
			}
		}
	}

	/**
	 * 既存ユーザーのコンストラクタで正しくレベルが与えられるかのテスト
	 */
	@Test
	public void exsistingZasshokuUserExpTest() {
		long userID = 1L;
		String screenName = "screenName";
		ZasshokuUser user;
		ZasshokuUser existingUser;

		for (int i = 0; i < 100; i++) {
			user = new ZasshokuUser(userID, screenName);
			user.gainExp(i);
			existingUser = new ZasshokuUser(userID, screenName, user.getTotalExp());

			// 同じパラメータを復元できるかテスト
			assertEquals(user.getTotalExp(), existingUser.getTotalExp());
			assertEquals(user.getLevel(), existingUser.getLevel());
			assertEquals(user.getNextLevelUpTotalExp(), existingUser.getNextLevelUpTotalExp());
			assertEquals(user.getNextLevelUpExp(), existingUser.getNextLevelUpExp());
		}
	}

	/**
	 * nextLevelUpExpのテスト
	 */
	@Test
	public void nextLevelUpExpTest() {
		long userID = 1L;
		String screenName = "screenName";
		ZasshokuUser user = new ZasshokuUser(userID, screenName);

		// 最初は2
		assertEquals(2, user.getNextLevelUpExp());

		// 経験値を1取得すると1減る
		user.gainExp(1);
		assertEquals(1, user.getNextLevelUpExp());

		// レベルアップすると増える
		user.gainExp(1);
		assertEquals(3, user.getNextLevelUpExp());

		// 一気に経験値を取得し、レベルアップすると増える
		user.gainExp(3);
		assertEquals(4, user.getNextLevelUpExp());

	}

	/**
	 * gainExp()の上限テスト
	 */
	@Test
	public void levelMaxTest() {
		long userID = 1L;
		String screenName = "screenName";
		ZasshokuUser user = new ZasshokuUser(userID, screenName);

		// 上限を超えて足してみる
		user.gainExp(Integer.MAX_VALUE);
		assertEquals(ZasshokuUser.MAX_LEVEL, user.getLevel());
		assertTrue(ZasshokuUser.MAX_EXP >= user.getTotalExp());

		// さらに足す
		user.gainExp(1);
		assertEquals(ZasshokuUser.MAX_LEVEL, user.getLevel());
		assertTrue(ZasshokuUser.MAX_EXP >= user.getTotalExp());

		// さらに足す
		user.gainExp(1);
		assertEquals(ZasshokuUser.MAX_LEVEL, user.getLevel());
		assertTrue(ZasshokuUser.MAX_EXP >= user.getTotalExp());

	}

}
