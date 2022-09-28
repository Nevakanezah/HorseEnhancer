package com.nevakanezah.horseenhancer.config

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.arim.dazzleconf.ConfigurationOptions
import space.arim.dazzleconf.error.ConfigFormatSyntaxException
import space.arim.dazzleconf.error.InvalidConfigException
import space.arim.dazzleconf.ext.snakeyaml.CommentMode
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlOptions
import space.arim.dazzleconf.helper.ConfigurationHelper
import java.util.logging.Level

class ConfigHandler(private val main: HorseEnhancerMain) {
    private val configFileName = "config.yml"

    var data: Config = fetchConfigData()
        private set

    private val configHelper = ConfigurationHelper(
        main.dataFolder.toPath(),
        configFileName,
        SnakeYamlConfigurationFactory.create(
            Config::class.java,
            ConfigurationOptions.defaults(),
            SnakeYamlOptions.Builder()
                .commentMode(CommentMode.fullComments())
                /*.yamlSupplier { Yaml(DumperOptions().apply {
                    indent = 2
                    isPrettyFlow = true
                    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                }) }*/
                .build()
        )
    )

    // Handles normal exceptions
    private fun fetchConfigData(): Config {
        return try {
            configHelper.reloadConfigData()
        } catch (e: ConfigFormatSyntaxException) {
            main.logger.log(Level.WARNING, "The YAML syntax in your config.yml is invalid.", e)
            configHelper.factory.loadDefaults()
        } catch (e: InvalidConfigException) {
            main.logger.log(Level.WARNING, "One of the values in your config.yml is invalid. Please check you have specified the right data types.", e)
            configHelper.factory.loadDefaults()
        }
    }

    fun reloadConfig() {
        data = fetchConfigData()
    }

    suspend fun reloadConfigException() = withContext(Dispatchers.IO) {
        data = try {
            configHelper.reloadConfigData()
        } catch (e: ConfigFormatSyntaxException) {
            configHelper.factory.loadDefaults()
            throw e
        } catch (e: InvalidConfigException) {
            configHelper.factory.loadDefaults()
            throw e
        }
    }
}
