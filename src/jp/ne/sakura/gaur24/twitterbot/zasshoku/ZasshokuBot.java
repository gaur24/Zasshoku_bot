package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import jp.ne.sakura.gaur24.twitterbot.api.FileIO;
import jp.ne.sakura.gaur24.twitterbot.api.PeriodicFollowCheckTimerTask;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;

/**
 * bot本体<br>
 * 各種タイマータスクを生成、スケジュールする
 */
public class ZasshokuBot {

	private static Timer timer = new Timer();

	public static void start(TwitterAPI twitterAPI, String zasshokuProperties) {

		Properties zasshokuP = FileIO.readProperty(zasshokuProperties);
		
		long delay = 5 * 1000L;

		// TimerTaskを設定
		// timer.schedule(TimerTask task, long delay, long period);
		// delay[ms]後にタスクを開始<br>
		// タスク開始後はperiod[ms]毎にタスクを実行します
		if (zasshokuP.getProperty("ZasshokuTweet") != null && zasshokuP.getProperty("ZasshokuTweet").equals("true")) {
			long tweetPeriod = TimeUnit.MINUTES.toMillis(Long.parseLong(zasshokuP.getProperty("ZasshokuTweetPeriod")));
			timer.schedule(new ZasshokuTweet(twitterAPI), delay, tweetPeriod);
		}
		if (zasshokuP.getProperty("ZasshokuReply") != null && zasshokuP.getProperty("ZasshokuReply").equals("true")) {
			List<ZasshokuUser> zasshokuUsers = new ArrayList<>();
			long replyPeriod = TimeUnit.MINUTES.toMillis(Long.parseLong(zasshokuP.getProperty("ZasshokuReplyPeriod")));
			timer.schedule(new ZasshokuReply(twitterAPI, zasshokuUsers), delay, replyPeriod);
		}
		if (zasshokuP.getProperty("FollowCheck") != null && zasshokuP.getProperty("FollowCheck").equals("true")) {
			long followCheckPeriod = TimeUnit.MINUTES
					.toMillis(Long.parseLong(zasshokuP.getProperty("FollowCheckPeriod")));
			timer.schedule(new PeriodicFollowCheckTimerTask(twitterAPI), delay, followCheckPeriod);
		}
		if (zasshokuP.getProperty("PiggybackingRetweet") != null && zasshokuP.getProperty("PiggybackingRetweet").equals("true")) {
			long piggybackingRetweetPeriod = TimeUnit.MINUTES
					.toMillis(Long.parseLong(zasshokuP.getProperty("PiggyBackingRetweetPeriod")));
			timer.schedule(new PeriodicPiggybackingRetweetTimerTask(twitterAPI), delay, piggybackingRetweetPeriod);
		}

	}

}
