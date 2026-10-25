package no.cas.allocator

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource


data class Config(val server: ServerConfig) {
    companion object {
        fun load(): Config {
            return ConfigLoaderBuilder.default()
                .addResourceSource("/application.yaml")
                .build()
                .loadConfigOrThrow<Config>()
        }
    }
}


data class ServerConfig(val port: Int)