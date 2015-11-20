package jp.ne.sakura.gaur24.twitterbot.zasshoku.markov;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import twitter4j.ResponseList;
import twitter4j.Status;

/**
 * タイムラインの文字列からMarkov連鎖による文章の再構築を制御する
 */
public class MarkovController {
	
	// ロガー
	private static final Logger logger = Logger.getLogger(MarkovController.class.getName());
	
	// \w : [0-9a-zA-Z_]と同じ
	// \W : \w以外の文字
	private static final String REGEX_MENTION = "@\\w+\\W";
	private static final String REGEX_RT = "RT[ ]@\\w+:[ ]";
	private static final String REGEX_HASHTAG = "#\\S+[　\\s]";
	
	/**
	 * markov連鎖を用いて文章を再構成し、結果を返す
	 * 生成に失敗するとnullを返す<br>
	 * 結果が返るまで、最大10秒ほど時間がかかる<br>
	 * 
	 * @param timeline
	 * @return String
	 */
	public static String getText(ResponseList<Status> timeline) {
		List<String> textList = new ArrayList<String>();
		for(Status status : timeline){
			// アドレスを含むツイートは取り除く
			if (status.getText().indexOf("http") != -1) {
				continue;
			}

			// 自分がつぶやいたツイートは取り除く
			if (status.getUser().getScreenName().equals(TwitterAPI.MY_SCREEN_NAME)) {
				continue;
			}

			// リツイート、ハッシュタグ、リプライの余分な文字を取り除く
			// 「RT @someone: 」「#some」「@someone」
			String text = status.getText().replaceAll(REGEX_RT, "").replaceAll(REGEX_HASHTAG, "")
					.replaceAll(REGEX_MENTION, "");

			// 文末にあるハッシュタグを消す
			text = text.split("#")[0];

			// TODO
			// デバッグ用。あとで消す
//			System.out.println(text);
			textList.add(text);
			
		}
		
		// 有効なツイートがなければ
		if (textList.size() <= 0) {
			logger.info("文章生成に適したツイートがありませんでした。");
			return null;
		}
		
		boolean success = false;
		for (int i = 0; i < 10; i++) {
			if (getMarkovText(textList)) {
				success = true;
				break;
			}
		}
		if (!success) {
			logger.info("文章の作成に失敗しました。");
			return null;
		}
		
		String result = MarkovGetterThread.getResult();
		if(result == null || result.isEmpty()){
			logger.info("文章の作成に失敗しました。");
			return null;
		}
		
		return result;
	}

	/**
	 * markov連鎖を用いて文章を再構成し、成功すればtrueを返す<br>
	 * 1秒待っても生成されない場合は失敗とみなし、falseを返す<br>
	 * 
	 * @param textList
	 * @return boolean
	 */
	private static boolean getMarkovText(List<String> textList) {
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

}
