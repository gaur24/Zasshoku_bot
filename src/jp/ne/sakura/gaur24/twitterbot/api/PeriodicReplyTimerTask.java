package jp.ne.sakura.gaur24.twitterbot.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
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

	// 1ユーザー当りのリプライを返す上限回数
	private final int REPLY_COUNT_LIMIT;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicReplyTimerTask.class.getName());

	// リプライ文が重複したらtrue;
	private boolean isDuplicate = false;

	// ユーザーIDと連続リプライ回数を保持する
	private Map<Long, Integer> replyCountMap = new HashMap<Long, Integer>();

	public PeriodicReplyTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;

		// TODO
		// プロパティから値をとってきたいけど、とりあえず。
		int replyCountLimit = 3;
		// 1より小さい値に設定した場合、実質上限なしとして扱う
		if (replyCountLimit < 1) {
			REPLY_COUNT_LIMIT = Integer.MAX_VALUE;
		} else {
			REPLY_COUNT_LIMIT = replyCountLimit;
		}
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
	abstract protected String createReply(Status replyStatus) throws TwitterException;

	/**
	 * このメソッドを実装し、つぶやきが成功したあとに実行する処理を記述してください<br>
	 * このメソッドは、createReply()によりリプライ文を生成し、つぶやきを実行した直後に呼ばれます<br>
	 * なんらかの例外が発生した場合、ログを残します
	 * 
	 * @param mention
	 * @throws TwitterException
	 */
	abstract protected void postProcessingOfSuccess(Status mention) throws TwitterException;

	@Override
	public void run() {

		// replyCountMapからremove予定のユーザーIDリスト
		Set<Long> removeUserIDSet = new HashSet<Long>();
		Set<Long> updateUserIDSet = new HashSet<Long>();

		try {
			ResponseList<Status> mentions = twitterAPI.getMentions(5);

			// ---------- リプライを行います ----------
			// 同じユーザーから何度もリプライが飛んできた場合は、
			// 1ユーザーに対して連続でREPLY_COUNT_LIMITまでしか返事しない
			// TODO
			// もっとシンプルなロジックにしたい

			// TODO
			// streamでうまいことできない？
			// リプライがきた時間の古い方から新しい方へ走査するためのiterator
			ListIterator<Status> iterator = mentions.listIterator(mentions.size());

			while (iterator.hasPrevious()) {
				Status mention = (Status) iterator.previous();

				// これからリプライを返すユーザーに、連続で何回返事をしたかを取得
				Integer count = replyCountMap.get(mention.getUser().getId());

				// nullの場合はカウント0とみなす
				if (count == null) {
					count = 0;
				}

				// TODO
				// debug
				System.out.println("count: " + count);

				// 返事回数の上限までなら返事をする
				if (count < REPLY_COUNT_LIMIT) {

					String createdReply = createReply(mention);

					// nullの場合は返事しない
					if (createdReply == null) {
						twitterAPI.doNotReplyToThisTweet(mention.getId());

						// removeするのは全部のリプライをチェックしてから
						removeUserIDSet.add(mention.getUser().getId());
						continue;
					}
					String reply = "@" + mention.getUser().getScreenName() + " " + createdReply;
					twitterAPI.postReply(reply, mention.getId(), isDuplicate);
					
					// つぶやきに成功するたびに呼ぶ
					postProcessingOfSuccess(mention);
					
					isDuplicate = false;

					// リプライチェック時に取得した複数のリプライの中に、同じユーザーからのリプライが複数含まれる場合、
					// 前回createReply()がnullだったとしても今回返事が出来る場合がある
					removeUserIDSet.remove(mention.getUser().getId());

					// リプライを返したユーザーを保持しておく
					updateUserIDSet.add(mention.getUser().getId());

					// 返事をしてカウントを増やす
					replyCountMap.put(mention.getUser().getId(), ++count);

				} else {
					// 連続して返事をする回数の上限を超えたため返事をしません
					logger.log(Level.FINE, "@" + mention.getUser().getScreenName() + " に連続で" + REPLY_COUNT_LIMIT
							+ "回リプライをしたため、こちらからの返事をやめます。");

					twitterAPI.doNotReplyToThisTweet(mention.getId());

					// 一度返事をしないと決めたユーザーは、今回のリプライチェックでは返事しない
					replyCountMap.put(mention.getUser().getId(), Integer.MAX_VALUE);

					// remove予定のユーザーIDリストに格納
					removeUserIDSet.add(mention.getUser().getId());

				}
			}

		} catch (TwitterException e) {
			if (e.isCausedByNetworkIssue()) {
				logger.log(Level.WARNING, "isCausedByNetworkIssue: ネットワークに問題があります。");
			} else if (e.exceededRateLimitation()) {
				logger.log(Level.WARNING, "exceededRateLimitation: API制限を超えました。");
			} else if (e.getErrorCode() == 187) {
				logger.log(Level.WARNING, "Status is a duplicate: 同じ文をツイートしようとしました。");
				isDuplicate = true;
			} else if (e.getErrorCode() == 186) {
				logger.log(Level.WARNING, "Status is over 140 characters: ツイートが140文字を超えています。");
			} else {
				logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
				e.printStackTrace();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "想定外のエラーが発生しています。" + e.getMessage());
			e.printStackTrace();
		} finally {
			// リプライを返している途中で例外が発生したら、それまでの分の情報でreplyCountMapを更新する

			// カウントを0にする予定にしたユーザーをreplyCountMapから削除
			for (long userID : removeUserIDSet) {
				replyCountMap.remove(userID);
			}

			// 更新されていない（リプライしていない）ユーザーをreplyCountMapから削除
			for (Long id : replyCountMap.keySet()) {
				boolean isFlag = false;
				// updateUserIDSetに該当するユーザーがいれば
				for (long updateID : updateUserIDSet) {
					if (id == updateID) {
						// 削除しない
						isFlag = true;
					}
				}
				if (!isFlag) {
					replyCountMap.remove(id);
				}
			}
		}

	}

}
