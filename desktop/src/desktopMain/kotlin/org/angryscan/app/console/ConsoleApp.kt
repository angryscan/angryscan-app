package org.angryscan.app.console

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import org.angryscan.app.common.AppVersion
import org.angryscan.app.console.commands.Scan
import org.angryscan.app.console.commands.Settings

class ConsoleApp: SuspendingCliktCommand() {
    override suspend fun run() {

    }
    init {
        installMordantMarkdown()
        versionOption(
            version = AppVersion,
            help = "Show app version",
            names = setOf("-v", "--version"),
            message = {
                "Angry Data Scanner version $it"
            }
        )

        subcommands(
            Scan(),
            Settings()
        )
    }
}