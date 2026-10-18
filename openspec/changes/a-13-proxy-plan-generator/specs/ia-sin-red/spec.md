## REMOVED Requirements

### Requirement: El plan sin red cabe en los minutos de sesión

**Reason**: Recortar el plan a los minutos de sesión es decidir su contenido, y eso es de la Lambda (plan §11.3). Era legítimo mientras el fake era la única implementación; con el proxy, el fake queda como doble de la red.

**Migration**: Con sesiones más cortas que las del ejemplo (75 min), el plan del fake no pasa la validación y la app enseña «plan inválido», igual que haría el proxy con ese ejemplo. Para ver un plan, el gimnasio de ejemplo de depuración o 75 minutos.
