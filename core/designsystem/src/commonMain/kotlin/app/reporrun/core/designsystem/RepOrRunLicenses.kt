package app.reporrun.core.designsystem

import app.reporrun.core.designsystem.resources.Res

/**
 * Las licencias de lo que este módulo empaqueta. El OFL 1.1 exige que su texto viaje con la
 * fuente: por eso está en `composeResources/files/`, junto a los TTF, y no solo en el repo.
 */
object RepOrRunLicenses {
    /** El aviso de copyright de Outfit y el texto íntegro del SIL Open Font License 1.1. */
    suspend fun outfit(): String = Res.readBytes("files/outfit_ofl.txt").decodeToString()
}
