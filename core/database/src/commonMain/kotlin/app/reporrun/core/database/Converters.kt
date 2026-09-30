package app.reporrun.core.database

import androidx.room.TypeConverter
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.ReviewMetric
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/*
 * Conversiones de columna. Los enums sueltos no pasan por aquí: Room los guarda por nombre.
 */

/**
 * - [Instant]: milisegundos desde epoch (`INTEGER`); se pierde lo que haya por debajo del
 *   milisegundo, que ningún dato de la app necesita.
 * - [LocalDate] y [LocalTime]: ISO 8601 (`2026-09-30`, `07:30`); la fecha ordena bien como texto.
 */
internal class DateConverters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
}

/**
 * Colecciones de escalares: array JSON en `TEXT` ([ScalarListCodec]). Los conjuntos de enums se
 * escriben en el orden del enum, así el mismo conjunto da siempre el mismo texto.
 */
internal class CollectionConverters {
    @TypeConverter
    fun stringsToText(value: List<String>?): String? = value?.let(ScalarListCodec::encodeStrings)

    @TypeConverter
    fun textToStrings(value: String?): List<String>? = value?.let(ScalarListCodec::decodeStrings)

    @TypeConverter
    fun intsToText(value: List<Int>?): String? = value?.let(ScalarListCodec::encodeInts)

    @TypeConverter
    fun textToInts(value: String?): List<Int>? = value?.let(ScalarListCodec::decodeInts)

    @TypeConverter
    fun daysToText(value: Set<DayOfWeek>?): String? = value?.let(::encodeEnums)

    @TypeConverter
    fun textToDays(value: String?): Set<DayOfWeek>? = value?.let { decodeEnums(it, DayOfWeek::valueOf) }

    @TypeConverter
    fun musclesToText(value: Set<MuscleGroup>?): String? = value?.let(::encodeEnums)

    @TypeConverter
    fun textToMuscles(value: String?): Set<MuscleGroup>? = value?.let { decodeEnums(it, MuscleGroup::valueOf) }

    @TypeConverter
    fun reviewMetricsToText(value: Set<ReviewMetric>?): String? = value?.let(::encodeEnums)

    @TypeConverter
    fun textToReviewMetrics(value: String?): Set<ReviewMetric>? = value?.let { decodeEnums(it, ReviewMetric::valueOf) }
}

private fun <E : Enum<E>> encodeEnums(values: Set<E>): String =
    ScalarListCodec.encodeStrings(values.sortedBy { it.ordinal }.map { it.name })

private fun <E : Enum<E>> decodeEnums(text: String, valueOf: (String) -> E): Set<E> =
    ScalarListCodec.decodeStrings(text).mapTo(LinkedHashSet(), valueOf)
