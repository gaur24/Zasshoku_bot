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

	// ファイルのパスを保持
	private final Path NOT_DESTROY_FRIENDSHIP_IDS_PATH;
	private final Path NOT_CREATE_FRIENDSHIP_IDS_PATH;
	private final Path LAST_REPLY_ID_PATH;

	// DestroyFriendshipしないユーザーIDのリスト
	private static List<Long> notDestroyFriendshipIDs;

	// CreateFriendshipしないユーザーIDのリスト
	private static List<Long> notCreateFriendshipIDs;

	// 最後にリプライしたツイートのID
	private static Long lastReplyID;

	/**
	 * 下記３つ情報を保持するファイルのパス名を渡してオブジェクトを生成する
	 * <ul>
	 * <li>フォローしないユーザーのIDリスト</li>
	 * <li>フォローを外さないユーザーのIDリスト</li>
	 * <li>前回リプライしたツイートのID</li>
	 * </ul>
	 * 
	 * @param twitter
	 * @param ncfip
	 * @param ndfip
	 * @param lrip
	 */
	public TwitterAPI(Twitter twitter, String ncfip, String ndfip, String lrip) {

		this.twitter = twitter;
		NOT_CREATE_FRIENDSHIP_IDS_PATH = Paths.get(ncfip);
		NOT_DESTROY_FRIENDSHIP_IDS_PATH = Paths.get(ndfip);
		LAST_REPLY_ID_PATH = Paths.get(lrip);

		// ファイル読み込み
		try {
			notCreateFriendshipIDs = FileIO.readAllLines(NOT_CREATE_FRIENDSHIP_IDS_PATH).stream().map(Long::valueOf)
					.collect(Collectors.toList());
		} catch (Exception e) {
			logger.log(Level.SEVERE, ncfip + "が読み込めませんでした");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			notDestroyFriendshipIDs = FileIO.readAllLines(NOT_DESTROY_FRIENDSHIP_IDS_PATH).stream().map(Long::valueOf)
					.collect(Collectors.toList());
		} catch (Exception e) {
			logger.log(Level.SEVERE, ndfip + "が読み込めませんでした");
			e.printStackTrace();
			System.exit(1);
		}

		// lastReplyIDだけ別処理で読み込み
		try {
			List<String> lastReplyIDList = FileIO.readAllLines(LAST_REPLY_ID_PATH);
			// LAST_REPLY_ID_PATHが読み込めない、または値がなかった場合
			if (lastReplyIDList == null || lastReplyIDList.isEmpty()) {
				// タイムラインの最新のツイートのIDをlastReplyIDとする
				Paging paging = new Paging(1, 1);
				ResponseList<Status> homeTimeLine = twitter.getHomeTimeline(paging);
				lastReplyID = homeTimeLine.get(0).getId();
				FileIO.write(LAST_REPLY_ID_PATH, lastReplyID.toString());
				logger.log(Level.WARNING, "タイムラインの最新のツイートのIDをlastReplyIDとして設定し、アプリケーションを実行します。");
			} else {
				lastReplyID = lastReplyIDList.stream().map(Long::valueOf).collect(Collectors.toList()).get(0);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "lastReplyIDが設定できませんでした: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * 自分のScreenNameを取得する API？
	 * 
	 * @return
	 * @throws TwitterException
	 */
	public String getMyScreenName() throws TwitterException {
		return twitter.getScreenName();
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
	 * @return ResponseList
	 * @throws TwitterException
	 */
	public ResponseList<Status> getHomeTimeline(int count) throws TwitterException {
		Paging paging = new Paging(1, count);
		return twitter.getHomeTimeline(paging);
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
		// リプライを返したツイートのIDを更新し、ファイルに書き込む
		if (lastReplyID < inReplyToStatusId) {
			lastReplyID = inReplyToStatusId;
			FileIO.write(LAST_REPLY_ID_PATH, lastReplyID.toString());
		}
	}

}
