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
public class PeriodicReplyTimerTask extends TimerTask {
	// Twitter API
	private TwitterAPI twitterAPI;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicReplyTimerTask.class.getName());

	// リプライ文が重複したらtrue;
	private boolean isDuplicate = false;

	public PeriodicReplyTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}

	@Override
	public void run() {
		try {
			ResponseList<Status> mentions = twitterAPI.getMentions(5);

			// 古いツイートから新しいツイートに向かって走査
			for (int i = mentions.size() - 1; i >= 0; i--) {
				String reply = "@" + mentions.get(i).getUser().getScreenName() + " " + "どもこん";
				twitterAPI.postReply(reply, mentions.get(i).getId(), isDuplicate);
				isDuplicate = false;
			}

		} catch (TwitterException e) {
			if (e.isCausedByNetworkIssue()) {
				logger.log(Level.WARNING, "isCausedByNetworkIssue: ネットワークに問題があります。再度リプライを試みます。");
			} else if (e.exceededRateLimitation()) {
				logger.log(Level.WARNING, "exceededRateLimitation: API制限を超えました。");
			} else if (e.getErrorCode() == 187) {
				logger.log(Level.WARNING, "Status is a duplicate: 同じ文をツイートしようとしました。スタンプを付加して再度リプライします。");
				isDuplicate = true;
			} else {
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。再度リプライを試みます。" + e.getMessage());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "想定外のエラーが発生しています。再度リプライを試みます。" + e.getMessage());
		}

	}

}
