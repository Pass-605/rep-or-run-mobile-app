package app.reporrun.core.data.repository

import app.reporrun.core.data.mapper.toEntity
import app.reporrun.core.data.mapper.toModel
import app.reporrun.core.database.dao.ProfileDao
import app.reporrun.core.domain.repository.PreferencesRepository
import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(private val dao: ProfileDao) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = dao.observeProfile().map { it?.toModel() }

    override suspend fun saveProfile(profile: UserProfile) = dao.upsertProfile(profile.toEntity())
}

class PreferencesRepositoryImpl(private val dao: ProfileDao) : PreferencesRepository {
    override fun observePreferences(): Flow<BlockPreferences?> = dao.observePreferences().map { it?.toModel() }

    override suspend fun savePreferences(preferences: BlockPreferences) = dao.upsertPreferences(preferences.toEntity())
}
