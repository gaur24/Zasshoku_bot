package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.nio.file.Paths;
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

		boolean isTweet = zasshokuP.getProperty("ZasshokuTweet", "").equals("true");
		boolean isReply = zasshokuP.getProperty("ZasshokuReply", "").equals("true");
		boolean isFollowCheck = zasshokuP.getProperty("FollowCheck", "").equals("true");
		boolean isRetweet = zasshokuP.getProperty("PiggybackingRetweet", "").equals("true");
		
		int zassyokuRatio = Integer.parseInt(zasshokuP.getProperty("ZassyokuRatio"));
		long zassyokuID = Long.parseLong(zasshokuP.getProperty("ZassyokuID"));

		// 5秒待って開始
		long delay = TimeUnit.SECONDS.toMillis(5);
		
		// TimerTaskを設定
		// timer.schedule(TimerTask task, long delay, long period);
		// delay[ms]後にタスクを開始<br>
		// タスク開始後はperiod[ms]毎にタスクを実行します
		try {
			if (isTweet) {
				long tweetPeriod = TimeUnit.MINUTES
						.toMillis(Long.parseLong(zasshokuP.getProperty("ZasshokuTweetPeriod")));
				timer.schedule(new ZasshokuTweet(twitterAPI, zassyokuRatio, zassyokuID), delay, tweetPeriod);
			}
			if (isReply) {
				List<String> zasshokuUsersStrings = FileIO.readAllLines(Paths.get("ZasshokuUsers.dat"));
				List<ZasshokuUser> zasshokuUsers = new ArrayList<>();
				if (zasshokuUsersStrings != null && !zasshokuUsersStrings.isEmpty()) {
					for (String line : zasshokuUsersStrings) {
						String[] elements = line.split(",");
						ZasshokuUser newUser = new ZasshokuUser(Long.parseLong(elements[0]), elements[1],
								Integer.parseInt(elements[3]));
						zasshokuUsers.add(newUser);
					}
				}
				long replyPeriod = TimeUnit.MINUTES
						.toMillis(Long.parseLong(zasshokuP.getProperty("ZasshokuReplyPeriod")));
				int replyCountLimit = Integer.parseInt(zasshokuP.getProperty("ZasshokuReplyCountLimit"));
				timer.schedule(new ZasshokuReply(twitterAPI, replyCountLimit, zasshokuUsers, "ZasshokuUsers.dat", zassyokuRatio, zassyokuID),
						delay, replyPeriod);
			}
			if (isFollowCheck) {
				long followCheckPeriod = TimeUnit.MINUTES
						.toMillis(Long.parseLong(zasshokuP.getProperty("FollowCheckPeriod")));
				timer.schedule(new PeriodicFollowCheckTimerTask(twitterAPI), delay, followCheckPeriod);
			}
			if (isRetweet) {
				long piggybackingRetweetPeriod = TimeUnit.MINUTES
						.toMillis(Long.parseLong(zasshokuP.getProperty("PiggyBackingRetweetPeriod")));
				timer.schedule(new PeriodicPiggybackingRetweetTimerTask(twitterAPI), delay, piggybackingRetweetPeriod);
			}
		} catch (Exception e) {
			System.err.println("zasshoku.propertiesまたはZasshokuUsers.datに不正な記述があります");
			e.printStackTrace();
			System.exit(1);
		}

	}

}
