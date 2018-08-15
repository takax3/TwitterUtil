package net.takax3.twitter.util

import net.arnx.jsonic.JSON
import twitter4j.*
import twitter4j.auth.AccessToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object RemoteWakeUp {
	
	private var consumerKey = ""
	private var consumerSecret = ""
	
	private var twitter: Twitter? = null
	private var twitterStream: TwitterStream? = null
	private var accessToken: AccessToken? = null
	
	
	@JvmStatic
	fun main(args: Array<String>) {
		
		val readConsumerKeys = ReadConsumerKeys()
		
		if (!readConsumerKeys.read("config/RemoteWakeUpConsumerKey.json")) {
			return
		}
		
		consumerKey = readConsumerKeys.consumerKey!!
		consumerSecret = readConsumerKeys.consumerSecret!!
		
		val file = File("config/RemoteWakeUpAccessToken.json")
		if (!file.exists()) {
			
			accessToken = OAuth().getAccessToken(consumerKey, consumerSecret)
			
			if (accessToken !is AccessToken) {
				return
			}
			
			try {
				val config = Config()
				config.accessToken = accessToken!!.token
				config.accessSecret = accessToken!!.tokenSecret
				
				val fileWriter: FileWriter? = FileWriter(file)
				
				fileWriter!!.write(JSON.encode(config))
				
				fileWriter.close()
				
				accessToken = AccessToken(config.accessToken!!, config.accessSecret!!)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
			
		} else {
			try {
				val fileReader: FileReader? = FileReader(file)
				
				val config = JSON.decode(fileReader, Config::class.java)
				fileReader!!.close()
				
				accessToken = AccessToken(config.accessToken!!, config.accessSecret!!)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
			
		}
		
		twitter = TwitterFactory().instance
		twitter!!.setOAuthConsumer(consumerKey, consumerSecret)
		twitter!!.oAuthAccessToken = accessToken
		twitterStream = TwitterStreamFactory().instance
		twitterStream!!.setOAuthConsumer(consumerKey, consumerSecret)
		twitterStream!!.oAuthAccessToken = accessToken
		twitterStream!!.addListener(Listener(twitter!!))
		
		twitterStream!!.user()
		
	}
	
	internal class Config {
		
		var accessToken: String? = null
		var accessSecret: String? = null
		
	}
	
}


internal class Listener(private var twitter: Twitter) : UserStreamListener {
	
	private var timeFormatter = SimpleDateFormat("HH:mm:ss:SSS")
	
	override fun onDeletionNotice(arg0: StatusDeletionNotice) {}
	
	override fun onScrubGeo(arg0: Long, arg1: Long) {}
	
	override fun onStallWarning(arg0: StallWarning) {}
	
	override fun onStatus(status: Status) {
		
		println("--------------------------------[New Tweet]")
		print(timeFormatter.format(tweetIdToDate(status.id)))
		println(" " + status.user.screenName + " " + status.user.name)
		println(status.text)
		println(status.id)
		
		val myStringID: String
		val myLongID: Long
		val relationship: Relationship
		
		val processBuilder: ProcessBuilder
		
		
		try {
			myStringID = twitter.screenName.toLowerCase()
			myLongID = twitter.id
		} catch (e: IllegalStateException) {
			e.printStackTrace()
			return
		} catch (e: TwitterException) {
			e.printStackTrace()
			return
		}
		
		if (status.user.id == myLongID) {
			return
		}
		
		if (status.text.toLowerCase().indexOf("@$myStringID") == -1) {
			return
		}
		
		
		try {
			relationship = twitter.showFriendship(myLongID, status.user.id)
		} catch (e: IllegalStateException) {
			e.printStackTrace()
			return
		} catch (e: TwitterException) {
			e.printStackTrace()
			return
		}
		
		if (!relationship.isTargetFollowingSource) {
			
			reply(twitter, status, "コマンドの実行権限がありません。")
			return
		}
		
		if (compareString(status, "/help")) {
			var string = BR
			string += "/help$BR"
			string += "/status$BR"
			string += "/run$BR"
			string += "/start [ProcessPath]$BR"
			reply(twitter, status, string)
			return
			
		}
		
		if (compareString(status, "/status")) {
			var string = BR
			string += "現在の実行環境$BR"
			string += BR
			string += "OS Name    : " + System.getProperty("os.name") + BR
			string += "OS Version : " + System.getProperty("os.version") + BR
			reply(twitter, status, string)
			return
		}
		
		if (compareString(status, "/start")) {
			try {
				if (status.text.toLowerCase().endsWith(".exe")) {
					processBuilder = ProcessBuilder(getCommandParameter(prepare(status.text).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), "/start"))
					processBuilder.start()
				} else {
					processBuilder = ProcessBuilder("cmd.exe", "/c", "start", getCommandParameter(prepare(status.text).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), "/start"))
					processBuilder.start()
				}
				reply(twitter, status, getCommandParameter(prepare(status.text).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), "/start") + "の起動に成功しました。")
			} catch (e: IOException) {
				e.printStackTrace()
				reply(twitter, status, e.toString())
			}
			
			return
		}
		
		if (compareString(status, "/run")) {
			try {
				processBuilder = ProcessBuilder("cmd.exe", "/c", "start", "run.bat")
				processBuilder.start()
				reply(twitter, status, "run.batの起動に成功しました。")
			} catch (e: IOException) {
				e.printStackTrace()
				reply(twitter, status, e.toString())
			}
			
			return
		}
	}
	
	override fun onTrackLimitationNotice(arg0: Int) {}
	
	override fun onException(arg0: Exception) {}
	
	override fun onBlock(arg0: User, arg1: User) {}
	
	override fun onDeletionNotice(arg0: Long, arg1: Long) {}
	
	override fun onDirectMessage(arg0: DirectMessage) {}
	
	override fun onFavorite(arg0: User, arg1: User, arg2: Status) {}
	
	override fun onFavoritedRetweet(arg0: User, arg1: User, arg2: Status) {}
	
	override fun onFollow(arg0: User, arg1: User) {}
	
	override fun onFriendList(arg0: LongArray) {}
	
	override fun onQuotedTweet(arg0: User, arg1: User, arg2: Status) {}
	
	override fun onRetweetedRetweet(arg0: User, arg1: User, arg2: Status) {}
	
	override fun onUnblock(arg0: User, arg1: User) {}
	
	override fun onUnfavorite(arg0: User, arg1: User, arg2: Status) {}
	
	override fun onUnfollow(arg0: User, arg1: User) {}
	
	override fun onUserDeletion(arg0: Long) {}
	
	override fun onUserListCreation(arg0: User, arg1: UserList) {}
	
	override fun onUserListDeletion(arg0: User, arg1: UserList) {}
	
	override fun onUserListMemberAddition(arg0: User, arg1: User, arg2: UserList) {}
	
	override fun onUserListMemberDeletion(arg0: User, arg1: User, arg2: UserList) {}
	
	override fun onUserListSubscription(arg0: User, arg1: User, arg2: UserList) {}
	
	override fun onUserListUnsubscription(arg0: User, arg1: User, arg2: UserList) {}
	
	override fun onUserListUpdate(arg0: User, arg1: UserList) {}
	
	override fun onUserProfileUpdate(arg0: User) {}
	
	override fun onUserSuspension(arg0: Long) {}
	
	
	private fun prepare(string: String): String {
		@Suppress("NAME_SHADOWING")
		var string = string
		
		string = string.replace(BR.toRegex(), " ")
		string = string.replace("　".toRegex(), " ")
		string = string.replace("	".toRegex(), " ")
		while (string.indexOf("  ") != -1) {
			string = string.replace(" {2}".toRegex(), " ")
		}
		
		return "$string "
		
	}
	
	private fun compareString(status: Status, include: String): Boolean {
		return status.text.toLowerCase().indexOf(include.toLowerCase()) != -1
	}
	
	private fun getCommandParameter(strings: Array<String>, command: String, i: Int = 1): String {
		
		var loop = 0
		
		while (true) {
			if (strings[loop].toLowerCase() == command.toLowerCase() && strings[loop + 1].toLowerCase() != command.toLowerCase()) {
				break
			}
			loop++
		}
		
		return strings[loop + i].replace("%20".toRegex(), " ")
		
	}
	
	private fun tweetIdToDate(ID: Long): Date {
		return Date(ID.ushr(22) + 1288834974657L)
	}
	
	private fun reply(twitter: Twitter, status: Status, string: String): Status? {
		
		val returnStatus: Status
		val statusUpdate = StatusUpdate("@" + status.user.screenName + string)
		statusUpdate.inReplyToStatusId = status.id
		
		try {
			returnStatus = twitter.updateStatus(statusUpdate)
		} catch (e: TwitterException) {
			println("-------------------------------[Exception]")
			println("予期せぬエラー:ツイートの投稿に失敗しました")
			e.printStackTrace()
			return null
		}
		
		return returnStatus
		
	}
	
	companion object {
		
		private val BR = System.getProperty("line.separator")
	}
	
}
