package jp.ne.sakura.gaur24.twitterbot.api;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * リプライを行うタイマータスク
 */
abstract public class PeriodicReplyTimerTask extends TimerTask {
	
	// Twitter API
	protected TwitterAPI twitterAPI;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicReplyTimerTask.class.getName());

	// リプライ文が重複したらtrue;
	private boolean isDuplicate = false;

	public PeriodicReplyTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}
	
	/**
	 * このメソッドを実装し、リプライする文章を作成してください<br>
	 * このメソッドの返り値に"@ユーザー名 "を含める必要はなく、自動で付加します<br>
	 * 返事をしない場合は、nullを返してください<br>
	 * なんらかの例外が発生した場合、ログを残します<br>
	 * 
	 * @param replyStatus
	 * @return String
	 * @throws TwitterException
	 */
	abstract public String makeReply(Status replyStatus) throws TwitterException;

	@Override
	public void run() {
		try {
			ResponseList<Status> mentions = twitterAPI.getMentions(5);

			// 古いツイートから新しいツイートに向かって走査
			for (int i = mentions.size() - 1; i >= 0; i--) {
				String makedReply = makeReply(mentions.get(i));
				// nullの場合は返事しない
				if(makedReply == null){
					continue;
				}
				String reply = "@" + mentions.get(i).getUser().getScreenName() + " " + makedReply;
				twitterAPI.postReply(reply, mentions.get(i).getId(), isDuplicate);
				isDuplicate = false;
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
