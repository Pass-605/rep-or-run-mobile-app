Cubre la tarea **C-11** de `../docs/backlog/C-diseno-ui.md` (Pantalla Generando): tres arreglos pedidos en la revisión, antes del PR.

## Why

La primera versión de la pantalla dejaba tres cosas flojas. La grave: la generación arrancaba al crearse el ViewModel, así que si el proceso moría con la espera delante, al restaurarse se pedía —y se guardaba— un plan que nadie había pedido. Estaba anotado para A-13, pero A-13 está en `moved:v1.1` y el fallo es de la pantalla, no del proxy.

## What Changes

- **Restaurarse no genera.** La primera generación deja una marca en el estado guardado de la entrada de la pila. Con la marca ya puesta al crearse el ViewModel no se arranca nada: la pantalla dice que la generación se interrumpió y ofrece «Generar otra vez». La interrupción no cuenta para el tope de dos.
- **Tras un límite de peticiones, el pie espera.** Con los segundos que da el servicio, «Reintentar» queda deshabilitado y con cuenta atrás; al pasar se habilita. Sin segundos, como antes.
- **Los problemas de validación van como detalle técnico**, bajo un rótulo que lo dice y sin acento, no como el mensaje de la pantalla.

## No objetivos

- Traducir los problemas a lenguaje de persona: sigue en «Condiciones antes de publicar».
- Extraer el armazón «cabecera propia + scroll + pie fijo»: va en C-12.
- Guardar el tope de dos para que sobreviva a la muerte del proceso.
- Cualquier cosa de C-01.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `pantalla-generando`: restaurarse tras morir el proceso, la espera tras un límite de peticiones y cómo se presentan los problemas.

## Impact

- **Código:** `feature:plan` y el test del grafo de Koin en `composeApp`.
- **Dependencias:** ninguna nueva. `SavedStateHandle` ya llegaba con Lifecycle.
- **Contrato, plan y Figma:** no se tocan.
- **Textos nuevos, a revisar antes de publicar:** la generación interrumpida (título y cuerpo), «Reintentar en N s», el cuerpo del límite de peticiones y el rótulo y la línea del detalle técnico.
