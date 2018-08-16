package net.takax3.twitter.util

import com.google.gson.GsonBuilder
import twitter4j.IDs
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


object MakeTimelineList {
	
	private var consumerKey = ""
	private var consumerSecret = ""
	
	private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	private var twitter: Twitter? = null
	private var accessToken: AccessToken? = null

	@JvmStatic
	fun main(args: Array<String>) {
		
		val readConsumerKeys = ReadConsumerKeys()
		
		if (!readConsumerKeys.read("config/MakeTimeLineListConsumerKey.json")) {
			return
		}
		
		consumerKey = readConsumerKeys.consumerKey!!
		consumerSecret = readConsumerKeys.consumerSecret!!
		

		val file = File("config/MakeTimeLineListAccessToken.json")
		if (!file.exists()) {
			
			accessToken = OAuth().getAccessToken(consumerKey, consumerSecret)
			
			if (accessToken !is AccessToken) {
				return
			}

			try {
				val config = Config()
				config.accessToken = accessToken!!.token
				config.accessSecret = accessToken!!.tokenSecret

				val fileWriter = FileWriter(file)

				fileWriter.write(gson.toJson(config))

				fileWriter.close()

				accessToken = AccessToken(config.accessToken, config.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}

		} else {
			try {
				val fileReader = FileReader(file)

				val config = gson.fromJson(fileReader, Config::class.java)
				fileReader.close()

				accessToken = AccessToken(config.accessToken, config.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}

		}
		
		twitter = TwitterFactory().instance
		twitter!!.setOAuthConsumer(consumerKey, consumerSecret)
		twitter!!.oAuthAccessToken = accessToken
		
		val list = twitter!!.createUserList("TimeLine-${SimpleDateFormat("yyyyMMddHHmmss").format(Date())}", false,
				"UserStream死亡に伴い、非公式アプリでのTL取得回数が15回/15分なのに対してList取得回数が900回/15分なのを生かしいつでもTLを見れるようにするList(Botでの自動生成)")
		println("Listを生成しました。")
		println("List名は ${list.name} です。")
		twitter!!.createUserListMember(list.id, twitter!!.id)
		
		var cursor: Long = -1
		var ids: IDs
		println("FollowingよりListの生成を行います。")
		do {
			ids = twitter!!.getFriendsIDs(cursor)
			for (id in ids.iDs.asList().asReversed()) {
				val user = twitter!!.showUser(id)
				println("@"+user.screenName + " ( " + user.name + " )")
				twitter!!.createUserListMember(list.id, id)
				Thread.sleep(1000)
			}
			println("以上 ${ids.iDs.size} 件")
			cursor = ids.nextCursor
		} while (cursor != 0L)
		println("Listのメンバー追加を完了しました。")
		

	}
	
	
	class Config {
		
		var accessToken: String? = null
		var accessSecret: String? = null
		
	}

}