package jp.ne.sakura.gaur24.twitterbot.api;

import java.util.Calendar;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		Calendar cal = Calendar.getInstance();
		try {
			twitterAPI.postTweet("今" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + "だよ");
		} catch (TwitterException e) {
			if (e.isCausedByNetworkIssue()) {
				logger.log(Level.WARNING, "isCausedByNetworkIssue: ネットワークに問題があります。");
			} else if (e.exceededRateLimitation()) {
				logger.log(Level.WARNING, "exceededRateLimitation: API制限を超えました。");
			} else if (e.getErrorCode() == 187) {
				logger.log(Level.WARNING, "Status is a duplicate: 同じ文をツイートしようとしました。");
			} else {
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
		}

	}

}
