package com.saveswitcher.domain.saf

import android.content.ContentResolver
import android.content.Context
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
            val sourceVariantName = "${game.baseName}_${sourceOwner}.${game.extension}"
            val sourceVariant = createOrReplaceFile(directory, sourceVariantName, backupExisting = true)
                ?: return@withContext "Could not archive current save"
            copyFile(baseFile, sourceVariant)
        }

        val targetVariantName = "${game.baseName}_${targetUserId}.${game.extension}"
        val targetVariant = directory.findFile(targetVariantName)

        if (targetVariant != null) {
            val refreshedBase = createOrReplaceFile(directory, baseFilename, backupExisting = true)
                ?: return@withContext "Could not activate target save"
            copyFile(targetVariant, refreshedBase)
            deleteWithBackup(directory, targetVariant.name)
            return@withContext "Switched to $targetUserId"
        }

        // No target variant yet: remove base so emulator can create a fresh save.
        deleteWithBackup(directory, baseFilename)
        return@withContext "No save for $targetUserId yet. A fresh save will be created by the emulator."
    }

    suspend fun exportCurrentSave(
        game: GameUiModel,
        exportFolderUri: String,
    ): String = withContext(Dispatchers.IO) {
        val gameDirectory = DocumentFile.fromTreeUri(context, Uri.parse(game.directoryUri))
            ?: return@withContext "Game directory is no longer accessible"
        val exportDirectory = DocumentFile.fromTreeUri(context, Uri.parse(exportFolderUri))
            ?: return@withContext "Export folder is no longer accessible"

        val baseFilename = "${game.baseName}.${game.extension}"
        val baseFile = gameDirectory.findFile(baseFilename)
            ?: return@withContext "No active save file to export"

        val exportName = "${game.baseName}_${FILE_TS_FORMATTER.format(Instant.now())}.${game.extension}"
        val exportFile = createUniqueFile(exportDirectory, exportName)
            ?: return@withContext "Could not create export file"
        copyFile(baseFile, exportFile)
        return@withContext "Exported to ${exportFile.name}"
    }

    suspend fun importSave(
        game: GameUiModel,
        importFileUri: String,
    ): String = withContext(Dispatchers.IO) {
        val gameDirectory = DocumentFile.fromTreeUri(context, Uri.parse(game.directoryUri))
            ?: return@withContext "Game directory is no longer accessible"

        val source = DocumentFile.fromSingleUri(context, Uri.parse(importFileUri))
            ?: return@withContext "Selected import file is no longer accessible"
        if (!source.isFile) {
            return@withContext "Selected item is not a file"
        }

        val baseFilename = "${game.baseName}.${game.extension}"
        val baseFile = createOrReplaceFile(gameDirectory, baseFilename, backupExisting = true)
            ?: return@withContext "Could not create destination save file"

        copyFile(source, baseFile)
        return@withContext "Imported save from ${source.name ?: "selected file"}"
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
                if (child.name == BACKUP_DIR_NAME) {
                    return@forEach
                }
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

    private fun createOrReplaceFile(
        parent: DocumentFile,
        name: String,
        backupExisting: Boolean,
    ): DocumentFile? {
        if (backupExisting) {
            createVersionedBackupIfExists(parent, name)
        }
        parent.findFile(name)?.delete()
        return parent.createFile("application/octet-stream", name)
    }

    private fun createVersionedBackupIfExists(parent: DocumentFile, targetName: String) {
        val existing = parent.findFile(targetName) ?: return
        if (!existing.isFile) return

        val backupDir = ensureBackupDirectory(parent) ?: return
        val backupName = versionedBackupName(targetName)
        val backupFile = createUniqueFile(backupDir, backupName) ?: return
        copyFile(existing, backupFile)
    }

    private fun deleteWithBackup(parent: DocumentFile, name: String?) {
        val targetName = name ?: return
        val existing = parent.findFile(targetName) ?: return
        if (!existing.isFile) {
            existing.delete()
            return
        }

        createVersionedBackupIfExists(parent, targetName)
        existing.delete()
    }

    private fun ensureBackupDirectory(parent: DocumentFile): DocumentFile? {
        parent.findFile(BACKUP_DIR_NAME)?.let { found ->
            if (found.isDirectory) {
                return found
            }
        }
        return parent.createDirectory(BACKUP_DIR_NAME)
    }

    private fun versionedBackupName(filename: String): String {
        val ts = FILE_TS_FORMATTER.format(Instant.now())
        val dot = filename.lastIndexOf('.')
        if (dot <= 0 || dot == filename.length - 1) {
            return "${filename}_$ts.bak"
        }
        val stem = filename.substring(0, dot)
        val ext = filename.substring(dot + 1)
        return "${stem}_${ts}.$ext.bak"
    }

    private fun createUniqueFile(parent: DocumentFile, name: String): DocumentFile? {
        var attempt = 0
        while (attempt < 1000) {
            val candidate = if (attempt == 0) {
                name
            } else {
                appendCounter(name, attempt)
            }
            if (parent.findFile(candidate) == null) {
                return parent.createFile("application/octet-stream", candidate)
            }
            attempt += 1
        }
        return null
    }

    private fun appendCounter(name: String, count: Int): String {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) {
            return "${name}_$count"
        }
        val stem = name.substring(0, dot)
        val ext = name.substring(dot + 1)
        return "${stem}_$count.$ext"
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
        const val BACKUP_DIR_NAME = "SaveSwitcherBackups"
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
        val FILE_TS_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
    }
}
