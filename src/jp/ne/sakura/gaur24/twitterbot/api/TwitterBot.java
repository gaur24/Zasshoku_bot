package jp.ne.sakura.gaur24.twitterbot.api;

import java.util.Timer;

public class TwitterBot {

	private TwitterAPI twitterAPI;
	private Timer timer;
	private boolean doPeriodicPost = true;
	private boolean doPeriodicReply = false;
	private boolean doPeriodicFollowCheck = false;

	private static long ONE_MINUTE = 60 * 1000;
	private long postPeriod = 10 * ONE_MINUTE;
	private long replyPeriod = 5 * ONE_MINUTE;
	private long followCheckPeriod = 12 * 60 * ONE_MINUTE;

	public TwitterBot(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}

	public void start() {
		timer = new Timer();

		if (doPeriodicPost) {
			timer.schedule(new PeriodicTweetTimerTask(twitterAPI), 0, postPeriod);
		}

		if (doPeriodicReply) {
			timer.schedule(new PeriodicReplyTimerTask(twitterAPI), 0, replyPeriod);
		}

		if (doPeriodicFollowCheck) {
			timer.schedule(new PeriodicFollowCheckTimerTask(twitterAPI), 0, followCheckPeriod);
		}
	}

}
