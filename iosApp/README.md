# iosApp

Envoltorio de iOS. El código Swift está listo, pero **falta crear el proyecto de Xcode**, que necesita un Mac.

Cuando haya uno:

1. Crear en Xcode una app de SwiftUI llamada `iosApp` en esta carpeta, sustituyendo los `.swift` generados por los de `iosApp/`.
2. Añadir el framework `ComposeApp` (Build Phases → New Run Script Phase con `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, y el framework en *Frameworks, Libraries and Embedded Content*).
3. Bundle identifier `app.reporrun`, deployment target iOS 15 o superior.

Tarea S-02 del backlog, etiquetada `needs:macos`.
