package jp.ne.sakura.gaur24.twitterbot.api;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * 主要なTwitter API提供するクラス
 */
public class TwitterAPI {

	private Twitter twitter;
	private static final Logger logger = Logger.getLogger(TwitterAPI.class.getName());

	/**
	 * Twitterでつぶやける最大文字数
	 */
	public static final int TWEET_LENGTH_MAX = 140;

	/**
	 * 自分のScreenName
	 */
	public static String MY_SCREEN_NAME = null;

	// ファイルのパスを保持
	private final Path NOT_DESTROY_FRIENDSHIP_IDS_PATH;
	private final Path NOT_CREATE_FRIENDSHIP_IDS_PATH;
	private final Path LAST_REPLY_ID_PATH;
	private final Path LAST_HOME_TIMELINE_ID_PATH;

	// DestroyFriendshipしないユーザーIDのリスト
	private static List<Long> notDestroyFriendshipIDs;

	// CreateFriendshipしないユーザーIDのリスト
	private static List<Long> notCreateFriendshipIDs;

	// 最後に取得したHomeTimelineのID
	private static Long lastHomeTimelineID;

	// 最後にリプライしたツイートのID
	private static Long lastReplyID;

	/**
	 * 下記4つの情報を保持するファイルのパス名を渡してオブジェクトを生成する
	 * <ul>
	 * <li>フォローしないユーザーのIDリスト</li>
	 * <li>フォローを外さないユーザーのIDリスト</li>
	 * <li>前回リプライしたツイートのID</li>
	 * <li>前回取得したホームタイムラインのツイートのID</li>
	 * </ul>
	 * 
	 * @param twitter
	 * @param notCreateFriendshipIDsPath
	 * @param notDestroyFriendshipIDsPath
	 * @param lastReplyIDPath
	 * @param lastHomeTimelineIDPath
	 */
	public TwitterAPI(Twitter twitter, String notCreateFriendshipIDsPath, String notDestroyFriendshipIDsPath,
			String lastReplyIDPath, String lastHomeTimelineIDPath) {

		this.twitter = twitter;
		NOT_CREATE_FRIENDSHIP_IDS_PATH = Paths.get(notCreateFriendshipIDsPath);
		NOT_DESTROY_FRIENDSHIP_IDS_PATH = Paths.get(notDestroyFriendshipIDsPath);
		LAST_REPLY_ID_PATH = Paths.get(lastReplyIDPath);
		LAST_HOME_TIMELINE_ID_PATH = Paths.get(lastHomeTimelineIDPath);

		// ファイル読み込み
		try {
			notCreateFriendshipIDs = FileIO.readAllLines(NOT_CREATE_FRIENDSHIP_IDS_PATH).stream().map(Long::valueOf)
					.collect(Collectors.toList());
		} catch (Exception e) {
			logger.log(Level.SEVERE, notCreateFriendshipIDsPath + "が読み込めませんでした");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			notDestroyFriendshipIDs = FileIO.readAllLines(NOT_DESTROY_FRIENDSHIP_IDS_PATH).stream().map(Long::valueOf)
					.collect(Collectors.toList());
		} catch (Exception e) {
			logger.log(Level.SEVERE, notDestroyFriendshipIDsPath + "が読み込めませんでした");
			e.printStackTrace();
			System.exit(1);
		}

		// lastReplyID, lastHomeTimelineIDだけ別処理で読み込み
		try {
			List<String> lastReplyIDList = FileIO.readAllLines(LAST_REPLY_ID_PATH);
			List<String> lastHomeTimelineIDList = FileIO.readAllLines(LAST_HOME_TIMELINE_ID_PATH);

			boolean isExistReplyID = true;
			boolean isExistHomeTimelineID = true;

			// ファイルが読み込めない、または値がなかった場合
			if (lastReplyIDList == null || lastReplyIDList.isEmpty()) {
				isExistReplyID = false;
			}
			if (lastHomeTimelineIDList == null || lastHomeTimelineIDList.isEmpty()) {
				isExistHomeTimelineID = false;
			}

			// タイムラインの最新のツイートのIDをlastReplyID, lastHomeTimelineIDとする
			ResponseList<Status> homeTimeLine = null;
			if (!isExistReplyID || !isExistHomeTimelineID) {
				Paging paging = new Paging(1, 1);
				homeTimeLine = twitter.getHomeTimeline(paging);
			}

			if (!isExistReplyID) {
				lastReplyID = homeTimeLine.get(0).getId();
				FileIO.write(LAST_REPLY_ID_PATH, lastReplyID.toString());
				logger.log(Level.WARNING, "タイムラインの最新のツイートのIDをlastReplyIDとして設定し、アプリケーションを実行します。");
			} else {
				lastReplyID = lastReplyIDList.stream().map(Long::valueOf).collect(Collectors.toList()).get(0);
			}

			if (!isExistHomeTimelineID) {
				lastHomeTimelineID = homeTimeLine.get(0).getId();
				FileIO.write(LAST_HOME_TIMELINE_ID_PATH, lastHomeTimelineID.toString());
				logger.log(Level.WARNING, "タイムラインの最新のツイートのIDをlastHomeTimelineIDとして設定し、アプリケーションを実行します。");
			} else {
				lastHomeTimelineID = lastHomeTimelineIDList.stream().map(Long::valueOf).collect(Collectors.toList())
						.get(0);
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, "lastReplyID, lastHomeTimelineIDが設定できませんでした: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		try {
			MY_SCREEN_NAME = twitter.getScreenName();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Twitterと接続できませんでした。" + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * フォロワーの全IDリストを取得<br>
	 * [GET followers/ids]APIをフォロワー約5000件に対して1消費する<br>
	 * 理論上は15*5000件を取得可能（15分に1回のみ）
	 * 
	 * @return List
	 * @throws TwitterException
	 */
	public List<Long> getFollowersIDs() throws TwitterException {
		List<Long> followersIDs = new ArrayList<Long>();
		IDs ids;
		long cursor = -1L;
		do {
			ids = twitter.getFollowersIDs(cursor);
			for (long id : ids.getIDs()) {
				followersIDs.add(id);
			}
			cursor = ids.getNextCursor();
		} while (ids.hasNext());
		return followersIDs;
	}

	/**
	 * フレンドの全IDリストを取得<br>
	 * [GET friends/ids]API（制限15回）をフレンド約5000件に対して1消費する<br>
	 * 理論上は15*5000件を取得可能（15分に1回のみ）
	 * 
	 * @return List
	 * @throws TwitterException
	 */
	public List<Long> getFriendIDsList() throws TwitterException {
		List<Long> friendsIDs = new ArrayList<Long>();
		IDs ids;
		long cursor = -1L;
		do {
			ids = twitter.getFriendsIDs(cursor);
			for (long id : ids.getIDs()) {
				friendsIDs.add(id);
			}
			cursor = ids.getNextCursor();
		} while (ids.hasNext());
		return friendsIDs;
	}

	/**
	 * フォローされたらフォローを返し、フォローを外されたらフォローを外す<br>
	 * notDestroyFriendshipIDsで指定されたユーザーのフォローは外さない<br>
	 * notCreateFriendshipIDsで指定されたユーザーはフォローしない<br>
	 * 既にフォロー申請済みの非公開ユーザーに対してフォローしようとした場合、そのユーザーのIDをnotCreateFriendshipIDsに追加し、
	 * ファイル書き込みする<br>
	 * <br>
	 * フォローを外すユーザーの数だけ以下のAPIをそれぞれ１回ずつ消費する<br>
	 * [POST friendship/destroy]API（制限なし）<br>
	 * [GET users/show]API（制限180回）<br>
	 * <br>
	 * フォローするユーザーの数だけ以下のAPIをそれぞれ1回ずつ消費する<br>
	 * [POST friendship/create]API（制限なし）<br>
	 * [GET users/show]API（制限180回）
	 * 
	 * @throws TwitterException
	 */
	public void followCheck() throws TwitterException {
		// フォロワーの全IDリストを取得
		List<Long> followersIDs = this.getFollowersIDs();

		// フレンドの全IDリストを取得
		List<Long> friendsIDs = this.getFriendIDsList();

		// フレンドでないユーザーをフォローから外す
		for (long userID : friendsIDs) {
			// フォロワーの全IDリストに含まれる場合
			if (followersIDs.contains(userID)) {
				continue;
			}
			// 指定したユーザーはフォローを外さない
			for (long id : notDestroyFriendshipIDs) {
				if (userID == id) {
					continue;
				}
			}
			twitter.destroyFriendship(userID);
			logger.log(Level.FINE, "ID:" + userID + " @" + twitter.showUser(userID).getScreenName() + "をフォローから外しました。");
		}

		// フォローリストにいないフォロワーをフォローする（フォロー返し）
		for (long userID : followersIDs) {
			// フレンドの全IDリストに含まれる場合
			if (friendsIDs.contains(userID)) {
				continue;
			}
			// 指定したユーザーはフォローしない
			for (long id : notCreateFriendshipIDs) {
				if (userID == id) {
					continue;
				}
			}

			try {
				twitter.createFriendship(userID);
				logger.log(Level.FINE, "ID:" + userID + " @" + twitter.showUser(userID).getScreenName() + "をフォローしました。");
			} catch (TwitterException e) {
				// 既にフォロー申請済みの非公開ユーザーをフォローしようとした場合
				if (e.getErrorCode() == 160) {
					// フォローしないユーザーリストとしてファイル書き込みする
					FileIO.writeAppend(NOT_CREATE_FRIENDSHIP_IDS_PATH, String.valueOf(userID));
					notCreateFriendshipIDs.add(userID);
					logger.log(Level.INFO, "ID:" + userID + " @" + twitter.showUser(userID).getScreenName()
							+ "は非公開設定されており、フォロー申請中のため次回からフォロー申請を行いません。");
				} else {
					throw new TwitterException(e);
				}
			}
		}
	}

	/**
	 * ホームタイムラインから最新count件を取得し、ResponseListに格納して返す<br>
	 * [GET statuses/home_timeline]APIを1消費する
	 * 
	 * @param count
	 *            <= 200
	 * @return ResponseList
	 * @throws TwitterException
	 */
	public ResponseList<Status> getHomeTimeline(int count) throws TwitterException {
		Paging paging = new Paging(1, count);
		return twitter.getHomeTimeline(paging);
	}

	/**
	 * ホームタイムラインから最新count件を取得し、ResponseListに格納して返す<br>
	 * このメソッドで取得した最新のツイートIDは保持され、再度タイムラインを取得する場合、前回からの更新分だけを返す [GET
	 * statuses/home_timeline]APIを1消費する
	 * 
	 * @param count
	 * @return ResponseList
	 * @throws TwitterException
	 */
	public ResponseList<Status> getHomeTimelineMemory(int count) throws TwitterException {
		Paging paging = new Paging(1, count, lastHomeTimelineID);
		ResponseList<Status> homeTimeline = twitter.getHomeTimeline(paging);
		// 本当にリストの最後が最新なんだっけ？
		// TODO
		Long lhtid = homeTimeline.get(homeTimeline.size() - 1).getId();
		if(lastHomeTimelineID < lhtid){
			lastHomeTimelineID = lhtid;
			FileIO.write(LAST_HOME_TIMELINE_ID_PATH, lastHomeTimelineID.toString());
		} else {
			// ロジックが確認できたらelseを消します
			System.out.println("getHomeTimelineMemory : ここが呼ばれるとするとロジックがおかしいので確認しないといけませんねぇ");
		}
		return homeTimeline;
	}

	/**
	 * つぶやきをpostします<br>
	 * [POST statuses/update]APIに制限はないが、1ユーザー当たりの投稿数の制限はある
	 * 
	 * @param s
	 * @throws TwitterException
	 */
	public void postTweet(String s) throws TwitterException {
		twitter.updateStatus(s);
		logger.log(Level.FINE, "【post】" + s);
	}

	/**
	 * リプライの最新count件を取得し、ResponseListに格納して返す<br>
	 * 取得したリプライのうち、前回リプライを返したツイートより古いものは取り除かれる<br>
	 * [GET statuses/mentions_timeline]APIを1消費する
	 * 
	 * @param count
	 *            <= 200
	 * @return ResponseList
	 * @throws TwitterException
	 */
	public ResponseList<Status> getMentions(int count) throws TwitterException {
		Paging paging = new Paging(1, count, lastReplyID);
		return twitter.getMentionsTimeline(paging);
	}

	/**
	 * リプライをpostします<br>
	 * リプライ文に[@ScreenName]を含めること<br>
	 * 特定のツイートに対するリプライをする場合、inReplyToStatusIdを設定する<br>
	 * あえて空リプをする場合はinReplyToStatusIdを-1とすること<br>
	 * リプライが前回のものと重複した場合はつぶやけないため、isDuplicateをtrueとすることで0〜99のスタンプを付加してリプライできる
	 * <br>
	 * [POST statuses/update]APIに制限はないが、1ユーザー当たりの投稿数の制限はある
	 * 
	 * @param reply
	 * @param inReplyToStatusId
	 * @param isDuplicate
	 * @throws TwitterException
	 */
	public void postReply(String reply, long inReplyToStatusId, boolean isDuplicate) throws TwitterException {
		// 最近のツイートと重複した文章をつぶやこうとした場合、スタンプを付加する
		if (isDuplicate) {
			Random rd = new Random();
			int stampNum = rd.nextInt(100);
			reply = reply + "" + stampNum;
		}
		if (reply.length() >= TWEET_LENGTH_MAX) {
			// ScreenNameが長い人等に返事できない場合があり、繰り返しリプライを失敗する状態を避けるため、リプライを諦める
			logger.log(Level.WARNING, "リプライ文が" + TWEET_LENGTH_MAX + "文字を超えています。返事をしません。");
		} else {
			StatusUpdate su = new StatusUpdate(reply);
			if (inReplyToStatusId > 0) {
				su.setInReplyToStatusId(inReplyToStatusId);
			}
			twitter.updateStatus(su);
			logger.log(Level.FINE, "【reply】" + reply);
		}

		updateLastReplyID(inReplyToStatusId);

	}
	
	/**
	 * 返事をしないリプライを通知します<br>
	 * 通知しない場合、getMentions()した時に再び取得されます
	 * 
	 * @param statusID
	 */
	public void doNotReplyToThisTweet(long statusID){
		// 内部的にはlastReplyIDを更新するだけ
		updateLastReplyID(statusID);
	}
	
	/**
	 * lastReplyIDを更新する<br>
	 * ファイル更新も同時に行う
	 * 
	 * @param statusID
	 */
	private void updateLastReplyID(long statusID){
		// 値が古ければ更新しない
		if(lastReplyID >= statusID){
			return;
		}
		lastReplyID = statusID;
		FileIO.write(LAST_REPLY_ID_PATH, lastReplyID.toString());
	}

	/**
	 * ツイートをリツイートします<br>
	 * [POST statuses/retweet/:id]APIに制限はないが、1ユーザー当たりの投稿数の制限はある
	 * 
	 * @param status
	 * @throws TwitterException
	 */
	public void retweetStatus(Status status) throws TwitterException {
		twitter.retweetStatus(status.getId());
		logger.log(Level.FINE, "【RT】" + status.getText());
	}

}
