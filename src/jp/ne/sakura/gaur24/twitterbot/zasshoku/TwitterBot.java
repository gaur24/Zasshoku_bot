package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import jp.ne.sakura.gaur24.twitterbot.api.PeriodicFollowCheckTimerTask;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;

/**
 * botの本体<br>
 * 各種タイマータスクを生成、スケジュールする
 */
public class TwitterBot {
	private TwitterAPI twitterAPI;
	private Timer timer;

	private boolean doPeriodicPost = true;
	private boolean doPeriodicReply = true;
	private boolean doPeriodicFollowCheck = false;
	private boolean doPeriodicRetweet = false;

	// タイマータスクを呼び出す時間間隔を設定
	// TODO プロパティファイルで設定可能に
	private long postPeriod = TimeUnit.HOURS.toMillis(1L);
	private long replyPeriod = TimeUnit.MINUTES.toMillis(3L);
	private long followCheckPeriod = TimeUnit.HOURS.toMillis(12L);
	private long retweetPeriod = TimeUnit.HOURS.toMillis(1L);

	public TwitterBot(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
		timer = new Timer();
	}

	public void start() {

		if (doPeriodicPost) {
			timer.schedule(new ZasshokuTweet(twitterAPI), 0, postPeriod);
		}

		if (doPeriodicReply) {
			timer.schedule(new ZasshokuReply(twitterAPI), 0, replyPeriod);
		}

		if (doPeriodicFollowCheck) {
			timer.schedule(new PeriodicFollowCheckTimerTask(twitterAPI), 0, followCheckPeriod);
		}

		if (doPeriodicRetweet) {
			timer.schedule(new PeriodicPiggybackingRetweetTimerTask(twitterAPI), 0, retweetPeriod);
		}
	}

}
