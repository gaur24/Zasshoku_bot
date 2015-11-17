package jp.ne.sakura.gaur24.twitterbot.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルの読み書きを行うクラス<br>
 * 例外発生時はnullまたはfalseを返し、アプリケーションの停止は行わない<br>
 * 文字コードは全てUTF-8を用いる
 */
public class FileIO {

	private static final Logger logger = Logger.getLogger(FileIO.class.getName());

	/**
	 * ファイル内の各行をStringオブジェクトとして読み込む<br>
	 * 例外発生時はnullを返す
	 * 
	 * @param path
	 * @return List
	 */
	public static List<String> readAllLines(Path path) {
		try {
			return Files.readAllLines(path);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "指定のファイルにアクセスできません: " + path.toString());
			e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.WARNING, "指定のファイルが見つかりません: " + path.toString());
			// e.printStackTrace();
		} catch (NullPointerException e) {
			logger.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Listに格納されたStringオブジェクトを１行ずつ書き込む<br>
	 * 例外発生時はfalseを返す
	 * 
	 * @param path
	 * @param lines
	 * @return true: success
	 */
	public static boolean write(Path path, List<String> lines) {
		return FileIO.write(path, lines, false);
	}

	/**
	 * Listに格納されたStringオブジェクトを追加(Append)で１行ずつ書き込む<br>
	 * 例外発生時はfalseを返す
	 * 
	 * @param path
	 * @param lines
	 * @return true: success
	 */
	public static boolean writeAppend(Path path, List<String> lines) {
		return FileIO.write(path, lines, true);
	}

	/**
	 * Stringオブジェクトを書き込む<br>
	 * 例外発生時はfalseを返す
	 * 
	 * @param path
	 * @param line
	 * @return true: success
	 */
	public static boolean write(Path path, String line) {
		try {
			List<String> lines = new ArrayList<String>();
			lines.add(line);
			return FileIO.write(path, lines);
		} catch (NullPointerException e) {
			logger.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Stringオブジェクトを追加(Append)で書き込む<br>
	 * 例外発生時はfalseを返す
	 * 
	 * @param path
	 * @param line
	 * @return true: success
	 */
	public static boolean writeAppend(Path path, String line) {
		try {
			List<String> lines = new ArrayList<String>();
			lines.add(line);
			return FileIO.writeAppend(path, lines);
		} catch (NullPointerException e) {
			logger.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * APPENDとして書き込むかどうかを選択し、Listに格納されたStringオブジェクトを１行ずつ書き込む<br>
	 * 例外発生時はfalseを返す
	 * 
	 * @param path
	 * @param lines
	 * @param isAppend
	 * @return true: success
	 */
	private static boolean write(Path path, List<String> lines, boolean isAppend) {
		try {
			if (isAppend) {
				Files.write(path, lines, StandardOpenOption.APPEND);
			} else {
				Files.write(path, lines);
			}
			return true;
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "指定のファイルにアクセスできません: " + path.toString());
			e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.WARNING, "I/Oエラーが発生しました: " + path.toString());
			e.printStackTrace();
		} catch (NullPointerException e) {
			logger.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * プロパティオブジェクトを読み込む<br>
	 * 例外発生時はnullを返す
	 * 
	 * @param fileName
	 * @return Propertiesオブジェクト
	 */
	public static Properties readProperty(String fileName) {
		Properties prop = new Properties();
		try (FileInputStream fis = new FileInputStream(fileName)) {
			prop.load(fis);
			return prop;
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "指定のファイルにアクセスできません: " + fileName);
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING, "指定のファイルが見つかりません: " + fileName);
			// e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.WARNING, "I/Oエラーが発生しました: " + fileName);
			e.printStackTrace();
		}
		return null;
	}
}
