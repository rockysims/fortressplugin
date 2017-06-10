package me.newyith.fortress.main

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.World
import org.bukkit.event.block.BlockBreakEvent
import java.util.*

class FortressPluginForWorld(val world: World) {
	val mapper = ObjectMapper() //TODO: consider moving this so we only need one instance of mapper
	init {
		mapper.visibilityChecker = mapper.serializationConfig.defaultVisibilityChecker
			.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
			.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
	}

	val generatorRunes = HashSet<GeneratorRune>()

	fun enable() {
		Log.log(world.name + " enable() called") //TODO: delete this line

		//load generatorRunes
		val saveLoadManager = FortressPlugin.getSaveLoadManager()
		generatorRunes.clear()
		generatorRunes.addAll(saveLoadManager.loadGeneratorRunes(world))
	}

	fun disable() {
		Log.log(world.name + " disable() called") //TODO: delete this line

		//save generatorRunes
		val saveLoadManager = FortressPlugin.getSaveLoadManager()
		generatorRunes.parallelStream().forEach {
			saveLoadManager.saveGeneratorRune(it)
		}
	}

	fun  onBlockBreakEvent(event: BlockBreakEvent) {
		Log.success("BlockBreakEvent at " + Point(event.block))
	}

//	fun saveTest() {
//		val plugin: JavaPlugin = FortressPlugin.getPlugin() ?: return
//
//
//		val testData = TestData(123)
//
//
//		val testFile = File(plugin.dataFolder, "test.json")
//		try {
//			val buffer = ByteArrayOutputStream()
//
//			//save to buffer
//			val dataMap = HashMap<String, Any>()
//			dataMap.put("testData", testData)
//			mapper.writeValue(buffer, dataMap)
//
//			//write buffer to file
//			val fos = FileOutputStream(testFile)
//			fos.write(buffer.toByteArray(), 0, buffer.size())
//			fos.close()
//		} catch (e: IOException) {
//			e.printStackTrace()
//		}
//	}
//
//	fun loadTest() {
//		Log.log("FortressPluginForWorld::load() called")
//		val plugin = FortressPlugin.getPlugin() ?: return
//
//		val testFile = File(plugin.dataFolder, "test.json")
//		try {
//			//if (test.json doesn't exist) make an empty test.json
//			if (!testFile.exists()) {
//				ObjectMapper().writeValue(testFile, HashMap<String, Any>()) // HashMap was LinkedHashMap
//			}
//
//			val dataMap = mapper.readValue<Map<*, *>>(testFile, Map::class.java)
//			Log.log("dataMap.size: " + dataMap.size)
//
//			val testData = mapper.convertValue<TestData>(dataMap["testData"], TestData::class.java)
//			Log.log("testData type: " + testData?.javaClass?.name)
//			if (testData is TestData) {
//
//
//				Log.log("----------------- loaded testData: " + testData.data)
//
//
//			}
//		} catch (e: Exception) {
//			e.printStackTrace()
//		}
//	}

}