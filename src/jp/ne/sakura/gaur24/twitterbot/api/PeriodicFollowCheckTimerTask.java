package jp.ne.sakura.gaur24.twitterbot.api;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import twitter4j.TwitterException;

public class PeriodicFollowCheckTimerTask extends TimerTask {

	// Twitter API
	private TwitterAPI twitterAPI;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicFollowCheckTimerTask.class.getName());

	public PeriodicFollowCheckTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}

	@Override
	public void run() {
		try {
			twitterAPI.followCheck();
		} catch (TwitterException e) {
			if (e.isCausedByNetworkIssue()) {
				logger.log(Level.WARNING, "isCausedByNetworkIssue: ネットワークに問題があります。");
			} else if (e.exceededRateLimitation()) {
				logger.log(Level.WARNING, "exceededRateLimitation: API制限を超えました。");
			} else {
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
		}

	}

}
