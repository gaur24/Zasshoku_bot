package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import jp.ne.sakura.gaur24.twitterbot.api.PeriodicReplyTimerTask;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import jp.ne.sakura.gaur24.twitterbot.zasshoku.markov.MarkovController;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ZasshokuReply extends PeriodicReplyTimerTask {

	public ZasshokuReply(TwitterAPI twitterAPI) {
		super(twitterAPI);
	}

	@Override
	public String makeReply(Status replyStatus) throws TwitterException {
		
		String screenName = replyStatus.getUser().getScreenName();
		
		// 自分からのリプライは無視する
		if(screenName.equals(TwitterAPI.MY_SCREEN_NAME)){
			return null;
		}
		
		// TODO
		// 連続で同じ人から5回くらいリプライがきたら返事をせずに会話を途切れさせる機能
		// bot同士の喧嘩防止
		// オンメモリで
		
		// Markov連鎖により文章を構成させ、生成結果を受け取る
		String reply = MarkovController.getText(twitterAPI.getHomeTimeline(200));

		// 文字数が140文字を超える場合、zasshoku_botが伝えきれないことを表現します
		// [@screenName reply]
		// TODO
		// 文字数の調節がうまくいってないので要修正
		if (reply.length() > TwitterAPI.TWEET_LENGTH_MAX - 3 - screenName.length() - 2) {
			reply = reply.substring(0, TwitterAPI.TWEET_LENGTH_MAX - 3 - screenName.length() - 2);
			reply += "文字数";
		}
		return reply;
	}

}
