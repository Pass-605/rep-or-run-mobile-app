# Semilla del catálogo de equipos

`equipment_catalog.json` es el inventario que ve la persona la primera vez que configura su gimnasio:
marca lo que tiene en lugar de escribirlo. Es **de solo lectura**: se carga al crear la base de datos
(`:core:database`) en la tabla `EquipmentCatalog` del plan §10. Lo que la persona añada a mano vive
en `Equipment` con `isCustom = true` y no toca este archivo.

No contiene datos de ninguna persona ni de ningún gimnasio concreto.

## Formato

```json
{
  "catalogVersion": 2,
  "description": "...",
  "items": [
    {
      "key": "pendulum_squat",
      "name": "Sentadilla péndulo",
      "nameEn": "Pendulum Squat",
      "aliases": ["Péndulo"],
      "category": "MACHINE",
      "loadType": "SLED",
      "primaryMuscles": ["QUADS", "GLUTES"]
    }
  ]
}
```

| Campo | Regla |
|---|---|
| `key` | Identificador estable en `snake_case`, único. **Nunca se renombra:** es lo que ata el equipo de una persona al catálogo. Si un nombre cambia, se cambia `name`, no `key`. |
| `name` | El nombre que se ve, en español. Entre 2 y 80 caracteres: es el que toma el aparato al marcarlo, y el que viaja a la Lambda en `Gym.equipment[].name`. |
| `nameEn` | En inglés, como lo rotula el gimnasio. No se muestra: sirve para buscar. Obligatorio. |
| `aliases` | Otras formas de buscarlo. Puede ir vacío. Ningún `name`, `nameEn` ni alias puede repetirse entre entradas: el buscador quedaría ambiguo. |
| `category` | `EquipmentCategory` del contrato. |
| `loadType` | `LoadType` del contrato: decide cómo se escribe y se compara la carga (plan §4.5). |
| `primaryMuscles` | `MuscleGroup` del contrato, uno o más, en orden de importancia. |

Los tres enums son los mismos que en `docs/contract/v1/common.schema.json`; en código son los enums
de `:core:model`, así que deserializar ya los valida.

## Estado actual

62 entradas: 29 máquinas, 7 de peso libre, 6 de cardio, 5 bancos, 5 de peso corporal, 4 de poleas,
5 otros y la Smith. Cubren los 14 grupos musculares y las 31 máquinas distintas del caso de
referencia TNT.

## Cómo añadir una entrada

1. Añádela al final del bloque de su familia, no al final del archivo.
2. `key` nueva y en `snake_case`; `name`, `nameEn` y alias que no choquen con los de otra entrada.
3. `loadType` correcto: es lo que hace que «30 kg» signifique lo mismo en el plan, en el check-in y
   en el PDF. Un carro (prensa, hack, péndulo) es `SLED` porque el vacío ya pesa; una mancuerna es
   `DUMBBELL` porque el peso es por mancuerna.
4. Sube `catalogVersion` solo si cambia la **forma** del archivo, no al añadir entradas.
5. Añadir entradas no migra nada: al actualizar la app se insertan las nuevas `key` que falten.

## Cómo corregir un nombre

Edita `name`, `nameEn` o `aliases` y deja la `key` como está. Al arrancar, la app pone al día el
texto de las entradas que ya tenía cargadas y el nombre de los aparatos que la persona marcó
desde el catálogo; las máquinas personalizadas y los planes guardados no cambian.

## Lo que comprueba el test de B-04

- JSON válido y `catalogVersion` conocida.
- `key` únicas y en `snake_case`.
- Ningún `name`, `nameEn` ni alias repetido entre entradas, ignorando mayúsculas y acentos.
- `nameEn` presente y `name` dentro de los límites del nombre de un aparato.
- `category`, `loadType` y `primaryMuscles` con valores del dominio, y `primaryMuscles` no vacío.
- Al menos 40 entradas y presencia de las familias: selector, discos, poleas, Smith, carro, banco,
  mancuerna, barra, cardio y peso corporal.
