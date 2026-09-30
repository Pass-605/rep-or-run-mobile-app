package app.reporrun.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Builder de iOS: el archivo va en la carpeta Documents de la app. */
fun repOrRunDatabaseBuilder(): RoomDatabase.Builder<RepOrRunDatabase> =
    Room.databaseBuilder<RepOrRunDatabase>(name = "${documentsDirectory()}/${RepOrRunDatabase.FILE_NAME}")

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return checkNotNull(url?.path) { "no se encontró la carpeta Documents" }
}
