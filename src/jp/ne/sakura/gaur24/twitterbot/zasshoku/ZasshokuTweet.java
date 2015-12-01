package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import jp.ne.sakura.gaur24.twitterbot.api.*;
import jp.ne.sakura.gaur24.twitterbot.zasshoku.markov.MarkovController;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ZasshokuTweet extends PeriodicTweetTimerTask {

	public ZasshokuTweet(TwitterAPI twitterAPI) {
		super(twitterAPI);
	}

	@Override
	public String makeTweet() throws TwitterException {
		
		ResponseList<Status> homeTimeline = twitterAPI.getHomeTimeline(200);
		
		// TODO
		// 雑食らしさの担保
		// 雑食のタイムラインを一定の割合で混ぜ、markovに一緒に投げる

		// Markov連鎖により文章を構成させ、生成結果を受け取る
		String tweet = MarkovController.getText(homeTimeline);

		// 文字数が140文字を超える場合、zasshoku_botが伝えきれないことを表現します
		// TODO
		// 文字数の調節がうまくいってないので要修正
		if (tweet.length() > TwitterAPI.TWEET_LENGTH_MAX - 3) {
			tweet = tweet.substring(0, TwitterAPI.TWEET_LENGTH_MAX - 3);
			tweet += "文字数";
		}
		return tweet;
	}
	
}
