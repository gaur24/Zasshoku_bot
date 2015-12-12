package jp.ne.sakura.gaur24.twitterbot.api;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import twitter4j.TwitterException;

/**
 * 定期的にツイートするタイマータスク
 */
public abstract class PeriodicTweetTimerTask extends TimerTask {

	// Twitter API
	protected TwitterAPI twitterAPI;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicTweetTimerTask.class.getName());

	public PeriodicTweetTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}
	
	/**
	 * このメソッドを実装し、定期的につぶやく文章を作成してください<br>
	 * なんらかの例外が発生した場合、ログを残します
	 * 
	 * @return String
	 * @throws TwitterException
	 */
	abstract public String makeTweet() throws TwitterException;

	@Override
	public void run() {
		try {
			String tweet = makeTweet();
			if(tweet == null || tweet.isEmpty()){
				logger.log(Level.WARNING, "つぶやきが正しく生成されていません。");
				return;
			}
			twitterAPI.postTweet(tweet);
			
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
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。", e);
			}
		} catch (IllegalArgumentException e){
			if(e.getMessage().contains("count should be positive integer")){
				logger.log(Level.SEVERE, "countの値は(1 <= count <= 200)としてください。", e);
			} else {
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。", e);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "想定外のエラーが発生しています。", e);
		}

	}

}
