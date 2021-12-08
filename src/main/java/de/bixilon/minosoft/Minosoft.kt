/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft

import de.bixilon.minosoft.config.profile.GlobalProfileManager
import de.bixilon.minosoft.config.profile.delegate.watcher.SimpleProfileDelegateLWatcher.Companion.profileWatch
import de.bixilon.minosoft.config.profile.profiles.eros.ErosProfileManager
import de.bixilon.minosoft.data.assets.JarAssetsManager
import de.bixilon.minosoft.data.assets.Resources
import de.bixilon.minosoft.data.language.LanguageManager.Companion.load
import de.bixilon.minosoft.data.language.MultiLanguageManager
import de.bixilon.minosoft.data.registries.DefaultRegistries
import de.bixilon.minosoft.data.registries.ResourceLocation
import de.bixilon.minosoft.data.registries.versions.Versions
import de.bixilon.minosoft.gui.eros.Eros
import de.bixilon.minosoft.gui.eros.XStartOnFirstThreadWarning
import de.bixilon.minosoft.gui.eros.crash.ErosCrashReport.Companion.crash
import de.bixilon.minosoft.gui.eros.util.JavaFXInitializer
import de.bixilon.minosoft.modding.event.events.FinishInitializingEvent
import de.bixilon.minosoft.modding.event.master.GlobalEventMaster
import de.bixilon.minosoft.modding.loading.ModLoader
import de.bixilon.minosoft.protocol.protocol.LANServerListener
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.terminal.AutoConnect
import de.bixilon.minosoft.terminal.CLI
import de.bixilon.minosoft.terminal.CommandLineArguments
import de.bixilon.minosoft.terminal.RunConfiguration
import de.bixilon.minosoft.util.*
import de.bixilon.minosoft.util.KUtil.fullName
import de.bixilon.minosoft.util.filewatcher.FileWatcherService
import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType
import de.bixilon.minosoft.util.task.pool.ThreadPool
import de.bixilon.minosoft.util.task.worker.StartupTasks
import de.bixilon.minosoft.util.task.worker.TaskWorker
import de.bixilon.minosoft.util.task.worker.tasks.Task


object Minosoft {
    val MAIN_THREAD: Thread = Thread.currentThread()
    val MINOSOFT_ASSETS_MANAGER = JarAssetsManager(Minosoft::class.java, mutableSetOf("minosoft"))
    val LANGUAGE_MANAGER = MultiLanguageManager()
    val START_UP_LATCH = CountUpAndDownLatch(1)

    @JvmStatic
    fun main(args: Array<String>) {
        CommandLineArguments.parse(args)
        Util.initUtilClasses()

        Log.log(LogMessageType.OTHER, LogLevels.INFO) { "Starting minosoft" }
        if (OSUtil.OS == OSUtil.OSs.MAC && !RunConfiguration.X_START_ON_FIRST_THREAD_SET && !RunConfiguration.DISABLE_RENDERING) {
            Log.log(LogMessageType.GENERAL, LogLevels.WARN) { "You are using MacOS. To use rendering you have to add the jvm argument §9-XstartOnFirstThread§r. Please ensure it is set!" }
        }
        GitInfo.load()

        val taskWorker = TaskWorker(criticalErrorHandler = { _, exception -> exception.crash() })


        taskWorker += Task(identifier = StartupTasks.LOAD_VERSIONS, priority = ThreadPool.HIGH, executor = {
            Log.log(LogMessageType.OTHER, LogLevels.VERBOSE) { "Loading versions..." }
            Versions.loadAvailableVersions(MINOSOFT_ASSETS_MANAGER.readLegacyJsonAsset(ResourceLocation(ProtocolDefinition.MINOSOFT_NAMESPACE, "mapping/versions.json")))
            Log.log(LogMessageType.OTHER, LogLevels.VERBOSE) { "Versions loaded!" }
        })

        taskWorker += Task(identifier = StartupTasks.LOAD_PROFILES, priority = ThreadPool.HIGH, dependencies = arrayOf(StartupTasks.LOAD_VERSIONS), executor = {
            Log.log(LogMessageType.PROFILES, LogLevels.VERBOSE) { "Loading profiles..." }
            GlobalProfileManager.initialize()
            Log.log(LogMessageType.PROFILES, LogLevels.INFO) { "Profiles loaded!" }
        })

        taskWorker += Task(identifier = StartupTasks.FILE_WATCHER, priority = ThreadPool.HIGH, optional = true, executor = {
            Log.log(LogMessageType.GENERAL, LogLevels.VERBOSE) { "Starting file watcher service..." }
            FileWatcherService.start()
            Log.log(LogMessageType.GENERAL, LogLevels.INFO) { "File watcher service started!" }
        })


        taskWorker += Task(identifier = StartupTasks.LOAD_LANGUAGE_FILES, dependencies = arrayOf(StartupTasks.LOAD_PROFILES), executor = {
            val language = ErosProfileManager.selected.general.language
            ErosProfileManager.selected.general::language.profileWatch(this, true) {
                Log.log(LogMessageType.OTHER, LogLevels.VERBOSE) { "Loading language files (${language.fullName})" }
                LANGUAGE_MANAGER.translators[ProtocolDefinition.MINOSOFT_NAMESPACE] = load(it, null, ResourceLocation(ProtocolDefinition.MINOSOFT_NAMESPACE, "language/"))
                Log.log(LogMessageType.OTHER, LogLevels.VERBOSE) { "Language files loaded!" }
            }
        })

        taskWorker += Task(identifier = StartupTasks.LOAD_DEFAULT_REGISTRIES, dependencies = arrayOf(StartupTasks.LOAD_PROFILES), executor = {
            Log.log(LogMessageType.OTHER, LogLevels.VERBOSE) { "Loading default registries..." }

            Resources.load()
            DefaultRegistries.load()

            Log.log(LogMessageType.OTHER, LogLevels.VERBOSE) { "Default registries loaded!" }
        })

        taskWorker += Task(identifier = StartupTasks.LOAD_MODS, dependencies = arrayOf(StartupTasks.LOAD_PROFILES), executor = { ModLoader.loadMods(it) })


        taskWorker += Task(identifier = StartupTasks.LISTEN_LAN_SERVERS, dependencies = arrayOf(StartupTasks.LOAD_PROFILES), executor = {
            LANServerListener.listen()
        })

        taskWorker += Task(identifier = StartupTasks.INITIALIZE_CLI, executor = { CLI.initialize() })

        if (!RunConfiguration.DISABLE_EROS) {
            taskWorker += Task(identifier = StartupTasks.INITIALIZE_JAVAFX, executor = { JavaFXInitializer.start() })
            taskWorker += Task(identifier = StartupTasks.X_START_ON_FIRST_THREAD_WARNING, executor = { XStartOnFirstThreadWarning.show() }, dependencies = arrayOf(StartupTasks.LOAD_PROFILES, StartupTasks.LOAD_LANGUAGE_FILES, StartupTasks.INITIALIZE_JAVAFX))

            // ToDo: Show start up progress window

            Util.forceClassInit(Eros::class.java)
        }


        taskWorker.work(START_UP_LATCH)

        START_UP_LATCH.dec() // remove initial count
        START_UP_LATCH.await()
        Log.log(LogMessageType.OTHER, LogLevels.INFO) { "All startup tasks executed!" }
        GlobalEventMaster.fireEvent(FinishInitializingEvent())


        RunConfiguration.AUTO_CONNECT_TO?.let { AutoConnect.autoConnect(it) }

        RenderPolling.pollRendering()
    }
}
