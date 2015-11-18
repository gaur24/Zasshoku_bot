package jp.ne.sakura.gaur24.twitterbot.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.LogManager;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class BotFactory {

	/**
	 * プロパティファイルからトークンを読み込み、Twitterインスタンスを生成し返す
	 * 
	 * @param tokenPropertyFile
	 * @return Twitter
	 */
	public Twitter getTwitterInstance(String tokenPropertyFile) {
		Properties token = FileIO.readProperty(tokenPropertyFile);

		// プロパティファイルから取得したトークンを用いて認証処理
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(token.getProperty("ConsumerKey"));
		cb.setOAuthConsumerSecret(token.getProperty("ConsumerSecret"));
		cb.setOAuthAccessToken(token.getProperty("AccessToken"));
		cb.setOAuthAccessTokenSecret(token.getProperty("AccessTokenSecret"));

		// Twitterのインスタンス化
		TwitterFactory tf = new TwitterFactory(cb.build());
		return tf.getInstance();
	}

	/**
	 * プロパティファイルを読み込み、TwitterAPIインスタンスを生成し返す
	 * 
	 * @param twitter
	 * @param confPropertyFile
	 * @return
	 */
	public TwitterAPI getTwitterAPIInstance(Twitter twitter, String confPropertyFile) {
		Properties conf = FileIO.readProperty(confPropertyFile);
		String ndfip = conf.getProperty("NotDestroyFriendshipIDsFile");
		String ncfip = conf.getProperty("NotCreateFriendshipIDsFile");
		String lrip = conf.getProperty("LastRepyIDFile");
		String lhip = conf.getProperty("LastHomeTimelineIDFile");
		return new TwitterAPI(twitter, ndfip, ncfip, lrip, lhip);
	}

	/**
	 * 任意のログプロパティを有効にする<br>
	 * mainメソッドの最初に呼び出すこと
	 * 
	 * @param loggingProperty
	 *            ログプロパティファイル名
	 */
	public void settingLoggerProperty(String loggingProperty) {
		try (final InputStream ins = getClass().getResourceAsStream(loggingProperty)) {
			if (ins == null) {
				System.err.println("ログプロパティファイル" + loggingProperty + "はクラスパス上に見つかりませんでした。");
				return;
			}
			LogManager.getLogManager().readConfiguration(ins);
			// System.err.println("LogManagerを設定しました。");
		} catch (IOException e) {
			System.err.println("LogManagerの設定中にI/O例外が発生しました。" + e.getMessage());
		}
	}

}
