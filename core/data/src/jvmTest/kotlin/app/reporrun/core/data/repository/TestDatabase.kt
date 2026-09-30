package app.reporrun.core.data.repository

import androidx.room.Room
import app.reporrun.core.data.PLAN_EQUIPMENT
import app.reporrun.core.data.equipment
import app.reporrun.core.data.gym
import app.reporrun.core.database.RepOrRunDatabase
import app.reporrun.core.database.buildRepOrRunDatabase
import kotlinx.coroutines.Dispatchers

/** Base en memoria con el mismo driver (SQLite bundled) que la app. */
fun inMemoryDatabase(): RepOrRunDatabase =
    Room.inMemoryDatabaseBuilder<RepOrRunDatabase>().buildRepOrRunDatabase(Dispatchers.IO)

/** El gimnasio de los fixtures con los aparatos que usa `plan()`. */
suspend fun RepOrRunDatabase.seedGymWithPlanEquipment() {
    val gyms = GymRepositoryImpl(gymDao(), equipmentDao())
    gyms.saveGym(gym())
    gyms.saveEquipment(PLAN_EQUIPMENT.map { equipment(it) })
}
