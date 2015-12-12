package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.util.List;
import java.util.Random;

import jp.ne.sakura.gaur24.twitterbot.api.PeriodicTweetTimerTask;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import jp.ne.sakura.gaur24.twitterbot.zasshoku.markov.MarkovController;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ZasshokuTweet extends PeriodicTweetTimerTask {
	
	// 雑食の割合[%]
	private final int ZASSYOKU_RATIO;
	
	// 雑食のユーザーID
	private final long ZASSYOKU_ID;
	
	// 突然雑食botからリプライを送り始める割合
	private final int START_REPLY_RATIO;
	
	private Random random = new Random();

	public ZasshokuTweet(TwitterAPI twitterAPI, int zassyokuRatio, long zassyokuID, int startReplyRatio) {
		super(twitterAPI);
		if(zassyokuRatio > 50){
			ZASSYOKU_RATIO = 50;
		} else {
			ZASSYOKU_RATIO = zassyokuRatio;
		}
		ZASSYOKU_ID = zassyokuID;
		if(startReplyRatio > 100){
			START_REPLY_RATIO = 100;
		} else {
			START_REPLY_RATIO = startReplyRatio;
		}
	}

	@Override
	public String makeTweet() throws TwitterException {
		
		ResponseList<Status> homeTimeline = twitterAPI.getHomeTimeline(200);
		
		// 雑食らしさの担保
		// 雑食のタイムラインを一定の割合で混ぜ、markovに一緒に投げる
		// 50%のときは、200取得して混ぜる
		if(ZASSYOKU_RATIO > 0){
			ResponseList<Status> zassyokuTimeline = twitterAPI.getUserTimeline(ZASSYOKU_ID, ZASSYOKU_RATIO * 4);
			homeTimeline.addAll(zassyokuTimeline);
		}

		// Markov連鎖により文章を構成させ、生成結果を受け取る
		String tweet = MarkovController.getText(homeTimeline);
		
		// 突然フォロワーにリプライをし始める機能
		if(START_REPLY_RATIO > random.nextInt(100)){
			List<Long> followers = twitterAPI.getFollowersIDs();
			String screenName = twitterAPI.showUser(followers.get(random.nextInt(followers.size()))).getScreenName();
			tweet = "@" + screenName + " " + tweet;
		}
		

		// 文字数が140文字を超える場合、zasshoku_botが伝えきれないことを表現します
		if (tweet.length() > TwitterAPI.TWEET_LENGTH_MAX - 3) {
			tweet = tweet.substring(0, TwitterAPI.TWEET_LENGTH_MAX - 3);
			tweet += "文字数";
		}
		return tweet;
	}
	
}
