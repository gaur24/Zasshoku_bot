package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * 便乗してリツイートするタイマータスク
 */
public class PeriodicPiggybackingRetweetTimerTask extends TimerTask {

	// Twitter API
	private TwitterAPI twitterAPI;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicPiggybackingRetweetTimerTask.class.getName());

	public PeriodicPiggybackingRetweetTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}

	@Override
	public void run() {
		try {
			ResponseList<Status> homeTimeline = twitterAPI.getHomeTimelineMemory(200);
			
			// 古いツイートから新しいツイートに向かって走査
			for (int i=homeTimeline.size() -1; i>= 0; i--){
				// リツイートで回ってきたツイートなら
				// TODO
				// すでに自分がリツイートしていれば？
				// すでに自分がリツイートしていれば？
				if(homeTimeline.get(i).isRetweet() || homeTimeline.get(0).isRetweeted() || homeTimeline.get(0).isRetweetedByMe()){
					continue;
				}
				// リツイートされた数が5より多いなら
				if(homeTimeline.get(i).getRetweetCount() > 5){
					twitterAPI.retweetStatus(homeTimeline.get(i).getId());
				}
				
			}
			
		} catch (TwitterException e) {
			if (e.isCausedByNetworkIssue()) {
				logger.log(Level.WARNING, "isCausedByNetworkIssue: ネットワークに問題があります。");
			} else if (e.exceededRateLimitation()) {
				logger.log(Level.WARNING, "exceededRateLimitation: API制限を超えました。");
			} else if (e.getErrorCode() == 187) {
				logger.log(Level.WARNING, "Status is a duplicate: 同じ文をツイートしようとしました。");
			} else if (e.getErrorCode() == 186) {
				logger.log(Level.WARNING, "Status is over 140 characters: ツイートが140文字を超えています。");
			} else {
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
		}

	}

}
