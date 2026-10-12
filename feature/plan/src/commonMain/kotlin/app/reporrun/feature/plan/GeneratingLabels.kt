package app.reporrun.feature.plan

import androidx.compose.runtime.Composable
import app.reporrun.core.domain.usecase.GenerationUnavailable
import app.reporrun.feature.plan.resources.Res
import app.reporrun.feature.plan.resources.generating_back_to_summary
import app.reporrun.feature.plan.resources.generating_error_interrupted_body
import app.reporrun.feature.plan.resources.generating_error_interrupted_title
import app.reporrun.feature.plan.resources.generating_error_invalid_input_body
import app.reporrun.feature.plan.resources.generating_error_invalid_input_title
import app.reporrun.feature.plan.resources.generating_error_invalid_plan_body
import app.reporrun.feature.plan.resources.generating_error_invalid_plan_limit_body
import app.reporrun.feature.plan.resources.generating_error_invalid_plan_limit_title
import app.reporrun.feature.plan.resources.generating_error_invalid_plan_title
import app.reporrun.feature.plan.resources.generating_error_network_body
import app.reporrun.feature.plan.resources.generating_error_network_title
import app.reporrun.feature.plan.resources.generating_error_rate_limited_body
import app.reporrun.feature.plan.resources.generating_error_rate_limited_body_unknown
import app.reporrun.feature.plan.resources.generating_error_rate_limited_title
import app.reporrun.feature.plan.resources.generating_error_timeout_body
import app.reporrun.feature.plan.resources.generating_error_timeout_title
import app.reporrun.feature.plan.resources.generating_error_upstream_body
import app.reporrun.feature.plan.resources.generating_error_upstream_title
import app.reporrun.feature.plan.resources.generating_generate_again
import app.reporrun.feature.plan.resources.generating_message_cancel
import app.reporrun.feature.plan.resources.generating_message_checked
import app.reporrun.feature.plan.resources.generating_message_sent
import app.reporrun.feature.plan.resources.generating_message_waiting
import app.reporrun.feature.plan.resources.generating_retry
import app.reporrun.feature.plan.resources.generating_review_data
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Del resultado a sus textos. Los `when` son exhaustivos sin `else`: un motivo nuevo en el caso
 * de uso no compila hasta que tiene su texto y su acción.
 */

/**
 * Los mensajes de la espera, en el orden en que rotan. Describen la espera y lo que la app hace
 * con la respuesta; ninguno dice nada de cómo será el plan, que es algo que nadie garantiza.
 */
internal val WaitMessages: List<StringResource> = listOf(
    Res.string.generating_message_sent,
    Res.string.generating_message_waiting,
    Res.string.generating_message_checked,
    Res.string.generating_message_cancel,
)

/** Lo que un error dice y lo que deja hacer al pie. */
internal class FailureTexts(
    val title: String,
    val body: String,
    val action: String,
    val actionEvent: GeneratingEvent,
    val problems: List<String> = emptyList(),
)

/**
 * **«Reintentar» y «Generar otra vez» no son la misma palabra a propósito:** lo primero repite
 * una petición que no llegó; lo segundo pide otro plan y cuesta otra llamada.
 */
@Composable
internal fun GenerationFailure.texts(): FailureTexts = when (this) {
    is GenerationFailure.Unavailable -> FailureTexts(
        title = stringResource(reason.title),
        body = reason.body(),
        action = stringResource(Res.string.generating_retry),
        actionEvent = GeneratingEvent.RetryClicked,
    )

    is GenerationFailure.InvalidPlan -> if (canGenerateAgain) {
        FailureTexts(
            title = stringResource(Res.string.generating_error_invalid_plan_title),
            body = stringResource(Res.string.generating_error_invalid_plan_body),
            action = stringResource(Res.string.generating_generate_again),
            actionEvent = GeneratingEvent.RetryClicked,
            problems = problems,
        )
    } else {
        FailureTexts(
            title = stringResource(Res.string.generating_error_invalid_plan_limit_title),
            body = stringResource(Res.string.generating_error_invalid_plan_limit_body),
            action = stringResource(Res.string.generating_back_to_summary),
            actionEvent = GeneratingEvent.CloseClicked,
            problems = problems,
        )
    }

    // La misma palabra que tras un plan inválido: aquí tampoco se repite nada, se pide otro plan.
    GenerationFailure.Interrupted -> FailureTexts(
        title = stringResource(Res.string.generating_error_interrupted_title),
        body = stringResource(Res.string.generating_error_interrupted_body),
        action = stringResource(Res.string.generating_generate_again),
        actionEvent = GeneratingEvent.RetryClicked,
    )

    is GenerationFailure.InvalidInput -> FailureTexts(
        title = stringResource(Res.string.generating_error_invalid_input_title),
        body = stringResource(Res.string.generating_error_invalid_input_body),
        action = stringResource(Res.string.generating_review_data),
        actionEvent = GeneratingEvent.CloseClicked,
        problems = problems,
    )
}

private val GenerationUnavailable.title: StringResource
    get() = when (this) {
        GenerationUnavailable.Network -> Res.string.generating_error_network_title
        GenerationUnavailable.Timeout -> Res.string.generating_error_timeout_title
        is GenerationUnavailable.RateLimited -> Res.string.generating_error_rate_limited_title
        GenerationUnavailable.Upstream -> Res.string.generating_error_upstream_title
    }

@Composable
private fun GenerationUnavailable.body(): String = when (this) {
    GenerationUnavailable.Network -> stringResource(Res.string.generating_error_network_body)

    GenerationUnavailable.Timeout -> stringResource(Res.string.generating_error_timeout_body)

    // Los segundos los trae el servicio; si no los dijo, no se inventan.
    is GenerationUnavailable.RateLimited -> when (val seconds = retryAfterSeconds) {
        null -> stringResource(Res.string.generating_error_rate_limited_body_unknown)
        else -> pluralStringResource(Res.plurals.generating_error_rate_limited_body, seconds, seconds)
    }

    GenerationUnavailable.Upstream -> stringResource(Res.string.generating_error_upstream_body)
}
