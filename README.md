# Zasshoku_bot
適当な言葉を喋るだけ

# HOW TO USE
* Twitter4jとlucene-gosenのライブラリを使用しています。
  
* token.propertiesにトークンを記入します。
* conf.propertiesに各種データを保持、設定するファイル名を書きます。通常はデフォルトのままでOK。
* notCreateFriendshipIDs.datに自動でフォローしないユーザーのIDを記入します。
* notDestroyFriendshipIDs.datに自動でフォローを外さないユーザーのIDを記入します。

自動フォローチェックを行う場合、フォローされたらフォローを返し、フォローを外されたらフォローを外すという動作を自動的に行います。  
notほげほげ.datファイルは、それらの例外を設定するためのファイルです。自身で設定する場合は、1行1ユーザーとして記入してください。  
また、非公開ユーザーにフォロー申請をした後、何度もフォロー申請しないようにするため、自動でnotCreateFriendshipIDs.datにユーザーIDを書き込みます。  
  
以下、雑食bot固有の機能として、zasshoku.propertiesにパラメータを記入します。 
ZassyokuID : 雑食本人のユーザーID  
ZassyokuRatio : 雑食の割合（0〜50％）  
ZasshokuTweet : trueなら定期的につぶやく機能を有効にします  
ZasshokuTweetPeriod : 定期的につぶやくときの時間間隔[minute]  
ZasshokuReply : trueなら自動でリプライを返す機能を有効にします  
ZasshokuReplyPeriod : リプライを返すときの時間間隔[minute]  
ZasshokuReplyCountLimit : 同じユーザーに連続でリプライ可能な上限回数（0以下なら制限なし）  
FollowCheck : 自動でフォローチェックする機能を有効にします  
FollowCheckPeriod : フォローチェックするときの時間間隔[minute]  
PiggybackingRetweet : 便乗してRetweetする機能を有効にします  
PiggybackingRetweetCount : 便乗してRetweetするRetweet回数の下限  
PiggybackingRetweetPeriod : 便乗してRetweetするときの時間間隔[minute]  
  
ZasshokuUsers.datに、各ユーザーの経験値を保持します。  
  
