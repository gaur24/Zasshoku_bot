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
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ZasshokuReply extends PeriodicReplyTimerTask {

	// ロガー
	private static final Logger logger = Logger.getLogger(ZasshokuReply.class.getName());

	private List<ZasshokuUser> zasshokuUsers;

	private final Path userPath;
	
	// 雑食の割合[%]
	private final int ZASSYOKU_RATIO;
	
	// 雑食のユーザーID
	private final long ZASSYOKU_ID;
	
	// 1回のリプライで取得できる経験値
	private static final int EXP_REPLY = 1;

	public ZasshokuReply(TwitterAPI twitterAPI, int replyCountLimit, List<ZasshokuUser> zasshokuUsers, String userPath, int zassyokuRatio, long zassyokuID) {
		super(twitterAPI, replyCountLimit);
		this.zasshokuUsers = zasshokuUsers;
		this.userPath = Paths.get(userPath);
		if(zassyokuRatio > 50){
			ZASSYOKU_RATIO = 50;
		} else {
			ZASSYOKU_RATIO = zassyokuRatio;
		}
		ZASSYOKU_ID = zassyokuID;
	}

	@Override
	public String createReply(Status replyStatus) throws TwitterException {

		String screenName = replyStatus.getUser().getScreenName();

		// 自分からのリプライは無視する
		if (screenName.equals(TwitterAPI.MY_SCREEN_NAME)) {
			return null;
		}
		
		ResponseList<Status> homeTimeline = twitterAPI.getHomeTimeline(200);
		
		// 雑食らしさの担保
		// 雑食のタイムラインを一定の割合で混ぜ、markovに一緒に投げる
		// 50%のときは、200取得して混ぜる
		if(ZASSYOKU_RATIO > 0){
			ResponseList<Status> zasshokuTimeline = twitterAPI.getUserTimeline(ZASSYOKU_ID, ZASSYOKU_RATIO * 4);
			homeTimeline.addAll(zasshokuTimeline);
		}

		// Markov連鎖により文章を構成させ、生成結果を受け取る
		String reply = MarkovController.getText(homeTimeline);

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
			
			int preLevel = user.getLevel();
			
			// 経験値を取得
			if (user.gainExp(EXP_REPLY)) {
				// レベルが上がったら
				String reply = "@" + mention.getUser().getScreenName() + " " + "レベルアップ！" + preLevel + "→" + user.getLevel() + " 次のレベルまであと"
						+ user.getNextLevelUpExp() + "exp";
				twitterAPI.postReply(reply, mention.getId(), false);
			}
			// ScreenNameを更新
			user.setScreenName(mention.getUser().getScreenName());
			logger.log(Level.FINE, "ユーザー @" + user.getScreenName() + "に経験値を" + EXP_REPLY + "付与しました");
			isExistingUser = true;
			break;
		}
		if (!isExistingUser) {
			ZasshokuUser newUser = new ZasshokuUser(mention.getUser().getId(), mention.getUser().getScreenName());
			newUser.gainExp(EXP_REPLY);
			zasshokuUsers.add(newUser);
			logger.log(Level.FINE, "ユーザー @" + newUser.getScreenName() + "を追加し、経験値を" + EXP_REPLY + "付与しました");
		}

		// toStringしてリストに格納
		List<String> zasshokuUsersToStrings = zasshokuUsers.stream().map(zasshokuUser -> zasshokuUser.toString())
				.collect(Collectors.toList());
		FileIO.write(userPath, zasshokuUsersToStrings);

	}

}
