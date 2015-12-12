package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import jp.ne.sakura.gaur24.twitterbot.api.BotFactory;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;

public class Main {

	public static void main(String[] args) {
		
		System.out.println("bot初期化中・・・");

		BotFactory botFactory = new BotFactory();
		botFactory.settingLoggerProperty("logging.properties");
		TwitterAPI twitterAPI = botFactory.getTwitterAPIInstance("token.properties", "conf.properties");

		ZasshokuBot.start(twitterAPI, "zasshoku.properties");

		System.out.println("bot開始");

	}

}
