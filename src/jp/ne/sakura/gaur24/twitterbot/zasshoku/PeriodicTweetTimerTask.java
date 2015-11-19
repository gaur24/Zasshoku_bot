package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import jp.ne.sakura.gaur24.twitterbot.zasshoku.markov.MarkovController;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class PeriodicTweetTimerTask extends TimerTask {

	// Twitter API
	private TwitterAPI twitterAPI;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicTweetTimerTask.class.getName());

	public PeriodicTweetTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}

	@Override
	public void run() {

		try {
			ResponseList<Status> homeTimeline = twitterAPI.getHomeTimeline(200);

			// Markov連鎖により文章を構成させ、生成結果を受け取る
			String result = MarkovController.getText(homeTimeline);

			if (result == null) {
				return;
			}

			// 文字数が140文字を超える場合、zasshoku_botが伝えきれないことを表現します
			if (result.length() > TwitterAPI.TWEET_LENGTH_MAX - 3) {
				result = result.substring(0, TwitterAPI.TWEET_LENGTH_MAX);
				result += "文字数";
			}
			twitterAPI.postTweet(result);

		} catch (TwitterException e) {
			if (e.isCausedByNetworkIssue()) {
				logger.log(Level.WARNING, "isCausedByNetworkIssue: ネットワークに問題があります。");
			} else if (e.exceededRateLimitation()) {
				logger.log(Level.WARNING, "exceededRateLimitation: API制限を超えました。");
			} else if (e.getErrorCode() == 187) {
				logger.log(Level.WARNING, "Status is a duplicate: 同じ文をツイートしようとしました。");
			} else if (e.getErrorCode() == 186) {
				logger.log(Level.WARNING, "Status is over 140 characters: ツイートが140文字を超えています。");
			} else {
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
		}

	}

}
