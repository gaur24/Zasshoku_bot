package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import jp.ne.sakura.gaur24.twitterbot.api.PeriodicReplyTimerTask;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ZasshokuReply extends PeriodicReplyTimerTask {

	public ZasshokuReply(TwitterAPI twitterAPI) {
		super(twitterAPI);
	}

	@Override
	public String makeReply(Status replyStatus) throws TwitterException {
		return "返事を考える機能はまだないんですよ〜";
	}

}
