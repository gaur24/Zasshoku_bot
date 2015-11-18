package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import jp.ne.sakura.gaur24.twitterbot.zasshoku.markov.MarkovGetterThread;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class PeriodicTweetTimerTask extends TimerTask {

	// Twitter API
	private TwitterAPI twitterAPI;

	// ロガー
	private static final Logger logger = Logger.getLogger(PeriodicTweetTimerTask.class.getName());

	// \w : [0-9a-zA-Z_]と同じ
	// \W : \w以外の文字
	private static final String REGEX_MENTION = "@\\w+\\W";
	private static final String REGEX_RT = "RT[ ]@\\w+:[ ]";
	private static final String REGEX_HASHTAG = "#\\S+[　\\s]";

	public PeriodicTweetTimerTask(TwitterAPI twitterAPI) {
		this.twitterAPI = twitterAPI;
	}

	/**
	 * markov連鎖を用いて文章を再構成し、成功すればtrueを返す<br>
	 * 1秒待っても生成されない場合は失敗とみなし、falseを返す<br>
	 * 
	 * @param textList
	 * @return boolean
	 */
	public boolean getMarkovText(List<String> textList) {
		MarkovGetterThread thread = new MarkovGetterThread(textList);
		thread.start();

		try {
			Thread.sleep(1000);
			if (thread.getState() != Thread.State.TERMINATED) {
				thread.stopThread();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void run() {

		try {
			ResponseList<Status> homeTimeline = twitterAPI.getHomeTimeline(200);
			List<String> textList = new ArrayList<String>(200);
			for (Status status : homeTimeline) {
				// アドレスを含むツイートは取り除く
				if (status.getText().indexOf("http") != -1) {
					continue;
				}

				// 自分がつぶやいたツイートは取り除く
				if (status.getUser().getScreenName().equals(twitterAPI.getMyScreenName())) {
					continue;
				}

				// リツイート、ハッシュタグ、リプライの余分な文字を取り除く
				// 「RT @someone: 」「#some」「@someone」
				String text = status.getText().replaceAll(REGEX_RT, "").replaceAll(REGEX_HASHTAG, "")
						.replaceAll(REGEX_MENTION, "");

				// 文末にあるハッシュタグを消す
				text = text.split("#")[0];

				System.out.println(text);
				textList.add(text);
			}
			// homeTimelineが取得できなかったら
			if (textList.size() <= 0) {
				logger.info("homeTimeline: 文章生成に適したツイートがなかったため今回はつぶやきません");
				return;
			}

			boolean success = false;
			for (int i = 0; i < 10; i++) {
				if (getMarkovText(textList)) {
					success = true;
					break;
				}
			}
			if (!success) {
				logger.info("markov: 文章作成に失敗したので今回はつぶやきません。");
				return;
			}

			// 生成結果を受け取る
			String result = MarkovGetterThread.getResult();
			
			// 文字数が140文字を超える場合、zasshoku_botが伝えきれないことを表現します
			if (result.length() > TwitterAPI.TWEET_LENGTH_MAX - 3) {
				result = result.substring(0, TwitterAPI.TWEET_LENGTH_MAX);
				result += "文字数";
			}
			twitterAPI.postTweet(result);

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
