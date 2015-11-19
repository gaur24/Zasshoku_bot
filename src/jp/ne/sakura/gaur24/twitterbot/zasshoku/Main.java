package jp.ne.sakura.gaur24.twitterbot.zasshoku;

import jp.ne.sakura.gaur24.twitterbot.api.BotFactory;
import jp.ne.sakura.gaur24.twitterbot.api.TwitterAPI;
import twitter4j.Twitter;

public class Main {

	public static void main(String[] args) {

		BotFactory botFactory = new BotFactory();
		botFactory.settingLoggerProperty("logging.properties");
		Twitter twitter = botFactory.getTwitterInstance("token.properties");
		TwitterAPI twitterAPI = botFactory.getTwitterAPIInstance(twitter, "conf.properties");

		TwitterBot bot = new TwitterBot(twitterAPI);
		bot.start();

	}

}
