package app.reporrun.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** Builder de Android: el archivo va en la carpeta de bases de datos de la app. */
fun repOrRunDatabaseBuilder(context: Context): RoomDatabase.Builder<RepOrRunDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<RepOrRunDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(RepOrRunDatabase.FILE_NAME).absolutePath,
    )
}
