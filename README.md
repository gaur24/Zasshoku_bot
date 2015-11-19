# Zasshoku_bot
適当な言葉を喋るだけ

# HOW TO USE
* Twitter4jとlucene-gosenのライブラリを使用しています。
  
* token.propertiesにトークンを記入します。それだけでとりあえず動きます。
* conf.propertiesに各種データを保持、設定するファイル名を書きます。通常はデフォルトのままでOK。
* notCreateFriendshipIDs.datに自動でフォローしないユーザーのIDを記入します。
* notDestroyFriendshipIDs.datに自動でフォローを外さないユーザーのIDを記入します。

自動フォローチェックを行う場合、フォローされたらフォローを返し、フォローを外されたらフォローを外すという動作を自動的に行います。  
notほげほげ.datファイルは、それらの例外を設定するためのファイルです。自身で設定する場合は、1行1ユーザーとして記入してください。  
また、非公開ユーザーにフォロー申請をした後、何度もフォロー申請しないようにするため、自動でnotCreateFriendshipIDs.datにユーザーIDを書き込みます。  
