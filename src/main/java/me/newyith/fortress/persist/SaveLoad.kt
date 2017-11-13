package me.newyith.fortress.persist

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import me.newyith.fortress.main.FortressPluginForWorld
import me.newyith.fortress.main.FortressPluginForWorlds
import me.newyith.fortress.rune.generator.GeneratorRuneId
import me.newyith.fortress.main.FortressPlugin
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.KClass

class SaveLoad {
	private val mapper = ObjectMapper()
	init {
		mapper.visibilityChecker = mapper.serializationConfig.defaultVisibilityChecker
			.withFieldVisibility(JsonAutoDetect.Visibility.NONE)
			.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
	}

	/**
	 * Build list of world names based on folders under worlds/ folder.
	 */
	fun getWorldNames(): Set<String> {
		val plugin = FortressPlugin.getPlugin() ?: return HashSet()
		val worldsFolder = File(plugin.dataFolder, "worlds")

		return if (worldsFolder.exists()) {
			worldsFolder.list().toSet()
		} else HashSet()
	}

	//---//

	inline fun <reified T : Any> load(path: String): T? = load(path, T::class)
	fun <T : Any> load(path: String, kind: KClass<T>): T? {
		val plugin = FortressPlugin.getPlugin() ?: return null

		var thing: T? = null
		try {
			val file = File(plugin.dataFolder, path + ".json")
			if (file.exists()) {
				thing = mapper.readValue<T>(file, kind.java)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return thing
	}

	fun save(thing: Any, path: String) {
		val plugin: JavaPlugin = FortressPlugin.getPlugin() ?: return

		val file = File(plugin.dataFolder, path + ".json")
		try {
			val buffer = ByteArrayOutputStream()

			//save to buffer
			mapper.writeValue(buffer, thing)

			//write buffer to file
			file.parentFile.mkdirs()
			val fos = FileOutputStream(file)
			fos.write(buffer.toByteArray(), 0, buffer.size())
			fos.close()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	companion object {
		fun getSavePathOfGeneratorRuneById(generatorRuneId: GeneratorRuneId): String {
			val worldName = generatorRuneId.worldName
			val anchorPoint = generatorRuneId.anchorPoint
			return "worlds/$worldName/generatorRunes/$anchorPoint"
		}

		fun getSavePathOfFortressPluginForWorld(worldName: String): String {
			return "worlds/$worldName/fortressPluginForWorld"
		}

		fun getSavePathOfFortressPluginForWorlds(): String {
			return "fortressPluginForWorlds"
		}
	}
}