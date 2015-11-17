package jp.ne.sakura.gaur24.twitterbot.zasshoku.markov;

import java.util.List;

public class MarkovGetterThread extends Thread {

	public static String resultString = "";

	public static String getResult() {
		return resultString;
	}

	private boolean threadRunning = false;

	private List<String> stringTable = null;

	public MarkovGetterThread(List<String> stringTable) {
		this.stringTable = stringTable;
		threadRunning = true;
	}

	public void stopThread() {
		threadRunning = false;
	}

	@Override
	public void run() {

		Markov marcov = new Markov(stringTable);

		while (threadRunning) {

			resultString = marcov.func();
			threadRunning = false;

		}

	}
}
