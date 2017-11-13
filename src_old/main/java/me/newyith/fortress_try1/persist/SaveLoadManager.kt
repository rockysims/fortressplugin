package me.newyith.fortress_try1.persist

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import me.newyith.fortress_try1.main.FortressPlugin
import me.newyith.fortress_try1.main.FortressPluginForWorld
import me.newyith.fortress_try1.rune.generator.GeneratorRune
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class SaveLoadManager {
	val mapper = ObjectMapper()
	init {
		mapper.visibilityChecker = mapper.serializationConfig.defaultVisibilityChecker
			.withFieldVisibility(JsonAutoDetect.Visibility.ANY) //TODO: change to NONE now that I'm always using @JsonProperty() for each field?
			.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
			.withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
	}

	fun createPluginByWorld(): Map<String, FortressPluginForWorld> {
		val plugin = FortressPlugin.getPlugin() ?: return HashMap()

		val pluginByWorld = HashMap<String, FortressPluginForWorld>()
		try {
			plugin.dataFolder.list().toList().parallelStream().forEach {
				Bukkit.getWorld(it)?.let { world ->
					pluginByWorld.put(world.name, FortressPluginForWorld(world))
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			pluginByWorld.clear()
		}

		return pluginByWorld
	}

	fun loadGeneratorRunes(world: World): Set<GeneratorRune> {
		val plugin = FortressPlugin.getPlugin() ?: return HashSet()

		val generatorRunes = HashSet<GeneratorRune>()
		try {
			val folder = File(plugin.dataFolder, world.name + "/generatorRunes")
			folder.list().toList().parallelStream().forEach {
				if (it.endsWith(".json")) {
					val file = File(folder, it)
					val generatorRune = mapper.readValue<GeneratorRune>(file, GeneratorRune::class.java)
					generatorRunes.add(generatorRune)
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			generatorRunes.clear()
		}

		generatorRunes.forEach { it.secondStageLoad() }

		return generatorRunes
	}

	fun saveGeneratorRune(generatorRune: GeneratorRune) {
		val plugin: JavaPlugin = FortressPlugin.getPlugin() ?: return

		val fileName = generatorRune.anchor.toString().replace(" ", "") + ".json"
		val path = generatorRune.world.name + "/generatorRunes/" + fileName
		val file = File(plugin.dataFolder, path)
		try {
			val buffer = ByteArrayOutputStream()

			//save to buffer
			mapper.writeValue(buffer, generatorRune)

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