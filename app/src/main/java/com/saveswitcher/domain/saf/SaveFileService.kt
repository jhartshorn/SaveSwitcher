package com.saveswitcher.domain.saf

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.saveswitcher.ui.model.EmulatorUiModel
import com.saveswitcher.ui.model.GameUiModel
import com.saveswitcher.ui.model.SaveFileUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SaveFileService(
    context: Context,
) {
    private val context: Context = context.applicationContext
    private val contentResolver: ContentResolver = context.contentResolver
    suspend fun scanGames(emulators: List<EmulatorUiModel>): List<GameUiModel> = withContext(Dispatchers.IO) {
        val games = linkedMapOf<String, MutableGame>()

        emulators.forEach { emulator ->
            val root = DocumentFile.fromTreeUri(context, Uri.parse(emulator.folderUri)) ?: return@forEach
            val allowed = emulator.extensions.map { it.trim().removePrefix(".").lowercase() }.toSet()
            walk(
                emulator = emulator,
                current = root,
                relativeDir = "",
                allowedExtensions = allowed,
                games = games,
            )
        }

        games.values.map { it.toUiModel() }.sortedBy { it.displayName.lowercase() }
    }

    suspend fun switchUser(
        game: GameUiModel,
        targetUserId: String,
        sourceOwnerUserId: String?,
    ): String = withContext(Dispatchers.IO) {
        val directory = DocumentFile.fromTreeUri(context, Uri.parse(game.directoryUri))
            ?: return@withContext "Game directory is no longer accessible"

        val baseFilename = "${game.baseName}.${game.extension}"
        val sourceOwner = sourceOwnerUserId?.trim().orEmpty()
        val baseFile = directory.findFile(baseFilename)

        if (targetUserId == sourceOwner && baseFile != null) {
            return@withContext "Already active for $targetUserId"
        }

        if (baseFile != null && sourceOwner.isBlank()) {
            return@withContext "Current save owner is required"
        }

        if (baseFile != null) {
            val backupName = "${baseFilename}.bak"
            val backupFile = createOrReplaceFile(directory, backupName)
                ?: return@withContext "Could not create backup file"
            copyFile(baseFile, backupFile)

            val sourceVariantName = "${game.baseName}_${sourceOwner}.${game.extension}"
            val sourceVariant = createOrReplaceFile(directory, sourceVariantName)
                ?: return@withContext "Could not archive current save"
            copyFile(baseFile, sourceVariant)
        }

        val targetVariantName = "${game.baseName}_${targetUserId}.${game.extension}"
        val targetVariant = directory.findFile(targetVariantName)

        if (targetVariant != null) {
            val refreshedBase = createOrReplaceFile(directory, baseFilename)
                ?: return@withContext "Could not activate target save"
            copyFile(targetVariant, refreshedBase)
            targetVariant.delete()
            return@withContext "Switched to $targetUserId"
        }

        // No target variant yet: remove base so emulator can create a fresh save.
        directory.findFile(baseFilename)?.delete()
        return@withContext "No save for $targetUserId yet. A fresh save will be created by the emulator."
    }

    private fun walk(
        emulator: EmulatorUiModel,
        current: DocumentFile,
        relativeDir: String,
        allowedExtensions: Set<String>,
        games: MutableMap<String, MutableGame>,
    ) {
        current.listFiles().forEach { child ->
            if (child.isDirectory) {
                val nextDir = if (relativeDir.isEmpty()) child.name.orEmpty() else "$relativeDir/${child.name.orEmpty()}"
                walk(emulator, child, nextDir, allowedExtensions, games)
                return@forEach
            }

            if (!child.isFile) return@forEach
            val filename = child.name ?: return@forEach
            if (filename.startsWith(".")) return@forEach
            if (filename.endsWith(".tmp", ignoreCase = true) || filename.endsWith(".bak", ignoreCase = true)) {
                return@forEach
            }

            val dotIndex = filename.lastIndexOf('.')
            if (dotIndex <= 0 || dotIndex == filename.length - 1) return@forEach
            val ext = filename.substring(dotIndex + 1)
            if (ext.lowercase() !in allowedExtensions) return@forEach

            val stem = filename.substring(0, dotIndex)
            val underscore = stem.lastIndexOf('_')
            val maybeUser = if (underscore > 0 && underscore < stem.length - 1) stem.substring(underscore + 1) else null
            val maybeBaseName = if (maybeUser != null) stem.substring(0, underscore) else stem

            val key = "${emulator.id}|$relativeDir|$maybeBaseName|$ext"
            val mutableGame = games.getOrPut(key) {
                MutableGame(
                    emulatorId = emulator.id,
                    emulatorName = emulator.name,
                    relativeDir = relativeDir,
                    baseName = maybeBaseName,
                    extension = ext,
                    directoryUri = current.uri.toString(),
                )
            }

            val info = SaveFileUiModel(
                uri = child.uri.toString(),
                filename = filename,
                modifiedAt = child.lastModified(),
                modifiedAtLabel = formatTimestamp(child.lastModified()),
            )

            if (maybeUser == null) {
                mutableGame.baseSave = info
            } else {
                mutableGame.userSaves[maybeUser] = info
            }
        }
    }

    private fun createOrReplaceFile(parent: DocumentFile, name: String): DocumentFile? {
        parent.findFile(name)?.delete()
        return parent.createFile("application/octet-stream", name)
    }

    private fun copyFile(source: DocumentFile, destination: DocumentFile) {
        contentResolver.openInputStream(source.uri).use { input ->
            contentResolver.openOutputStream(destination.uri, "wt").use { output ->
                if (input != null && output != null) {
                    input.copyTo(output)
                    output.flush()
                }
            }
        }
    }

    private fun formatTimestamp(value: Long): String {
        if (value <= 0L) return "Unknown"
        return DATE_FORMATTER.format(Instant.ofEpochMilli(value))
    }

    private data class MutableGame(
        val emulatorId: String,
        val emulatorName: String,
        val relativeDir: String,
        val baseName: String,
        val extension: String,
        val directoryUri: String,
        var baseSave: SaveFileUiModel? = null,
        val userSaves: MutableMap<String, SaveFileUiModel> = linkedMapOf(),
    ) {
        fun toUiModel(): GameUiModel {
            return GameUiModel(
                id = "$emulatorId|$relativeDir|$baseName|$extension",
                emulatorId = emulatorId,
                emulatorName = emulatorName,
                relativeDir = relativeDir,
                baseName = baseName,
                extension = extension,
                directoryUri = directoryUri,
                displayName = baseName,
                baseSave = baseSave,
                userSaves = userSaves.toMap(),
            )
        }
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
    }
}
