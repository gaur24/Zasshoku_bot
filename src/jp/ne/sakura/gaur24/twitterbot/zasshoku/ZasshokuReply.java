package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jp.ne.sakura.gaur24.twitterbot.api.FileIO;
import jp.ne.sakura.gaur24.twitterbot.api.PeriodicReplyTimerTask;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import jp.ne.sakura.gaur24.twitterbot.zasshoku.markov.MarkovController;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ZasshokuReply extends PeriodicReplyTimerTask {

	// ロガー
	private static final Logger logger = Logger.getLogger(ZasshokuReply.class.getName());

	private List<ZasshokuUser> zasshokuUsers;

	private final Path userPath;

	public ZasshokuReply(TwitterAPI twitterAPI, List<ZasshokuUser> zasshokuUsers, String userPath) {
		super(twitterAPI);
		this.zasshokuUsers = zasshokuUsers;
		this.userPath = Paths.get(userPath);
	}

	@Override
	public String createReply(Status replyStatus) throws TwitterException {

		String screenName = replyStatus.getUser().getScreenName();

		// 自分からのリプライは無視する
		if (screenName.equals(TwitterAPI.MY_SCREEN_NAME)) {
			return null;
		}

		// Markov連鎖により文章を構成させ、生成結果を受け取る
		String reply = MarkovController.getText(twitterAPI.getHomeTimeline(200));

		// 文字数が140文字を超える場合、zasshoku_botが伝えきれないことを表現します
		// [@screenName reply]
		if (reply.length() > TwitterAPI.TWEET_LENGTH_MAX - 3 - screenName.length() - 2) {
			reply = reply.substring(0, TwitterAPI.TWEET_LENGTH_MAX - 3 - screenName.length() - 2);
			reply += "文字数";
		}
		return reply;
	}

	@Override
	protected void postProcessingOfSuccess(Status mention) throws TwitterException {

		boolean isExistingUser = false;

		for (ZasshokuUser user : zasshokuUsers) {
			if (user.getUserID() != mention.getUser().getId()) {
				continue;
			}
			// 経験値を取得
			if (user.gainExp(1)) {
				// レベルが上がったら
				String reply = "@" + mention.getUser().getScreenName() + " " + "レベルアップ！" + (user.getLevel() - 1) + "→" + user.getLevel() + " 次のレベルまであと"
						+ user.getNextLevelUpExp() + "exp";
				twitterAPI.postReply(reply, mention.getId(), false);
			}
			// ScreenNameを更新
			user.setScreenName(mention.getUser().getScreenName());
			logger.log(Level.FINE, "ユーザー @" + user.getScreenName() + "に経験値を1付与しました");
			System.out.println(user.toString());
			isExistingUser = true;
			break;
		}
		if (!isExistingUser) {
			ZasshokuUser newUser = new ZasshokuUser(mention.getUser().getId(), mention.getUser().getScreenName());
			newUser.gainExp(1);
			zasshokuUsers.add(newUser);
			logger.log(Level.FINE, "ユーザー @" + newUser.getScreenName() + "を追加しました");
			System.out.println(newUser.toString());
		}

		// toStringしてリストに格納
		List<String> zasshokuUsersToStrings = zasshokuUsers.stream().map(zasshokuUser -> zasshokuUser.toString())
				.collect(Collectors.toList());
		FileIO.write(userPath, zasshokuUsersToStrings);

	}

}
