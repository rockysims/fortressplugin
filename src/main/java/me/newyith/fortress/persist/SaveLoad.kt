package me.newyith.fortress.persist

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import me.newyith.fortress_try1.main.FortressPlugin
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

	inline fun <reified T : Any> load(path: String): T? = load(T::class, path)
	fun <T : Any> load(kind: KClass<T>, path: String): T? {
		val plugin = FortressPlugin.getPlugin() ?: return null

		var thing: T? = null
		try {
			val file = File(plugin.dataFolder, path + ".json")
			thing = mapper.readValue<T>(file, kind.java)
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
}