#!/usr/bin/env bash
# Copia el contrato desde ../docs/contract (la fuente de verdad, fuera de git) a contract/v1/.
#
#   scripts/sync-contract.sh           copia y deja contract/v1/ idéntico a la fuente
#   scripts/sync-contract.sh --check   solo compara; sale con 1 si difieren, sin escribir
#
# En la fuente los ejemplos son hermanos de v1/ (docs/contract/examples/); aquí cuelgan de la
# versión (contract/v1/examples/). Solo viajan los schemas y los ejemplos: el README y
# validate.py se quedan en docs/.
#
# CONTRACT_SOURCE cambia el origen (por defecto, ../docs/contract respecto a la raíz del repo).
set -euo pipefail

VERSION="v1"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="${CONTRACT_SOURCE:-$REPO_ROOT/../docs/contract}"
TARGET="$REPO_ROOT/contract/$VERSION"

usage() {
  echo "Uso: scripts/sync-contract.sh [--check]" >&2
  exit 2
}

CHECK=false
case "${1:-}" in
  "") ;;
  --check) CHECK=true ;;
  *) usage ;;
esac
[ "$#" -le 1 ] || usage

missing=()
[ -d "$SOURCE/$VERSION" ] || missing+=("$SOURCE/$VERSION (los *.schema.json)")
[ -d "$SOURCE/examples" ] || missing+=("$SOURCE/examples (los ejemplos, con invalid/ dentro)")
if [ "${#missing[@]}" -gt 0 ]; then
  {
    echo "sync-contract: no encuentro la fuente del contrato. Falta:"
    printf '  - %s\n' "${missing[@]}"
    echo "Se espera el workspace completo, con docs/ al lado del repo: ../docs/contract."
    echo "docs/ es local y no está en git; si la tienes en otro sitio, usa CONTRACT_SOURCE=<ruta>."
    echo "Para compilar y pasar los tests no hace falta: usan la copia de contract/$VERSION/."
  } >&2
  exit 1
fi

# Se monta la copia en un directorio temporal con la forma del repo; así copiar y comparar
# parten de lo mismo y un archivo borrado en la fuente también se detecta.
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
cp "$SOURCE/$VERSION"/*.schema.json "$STAGE/"
cp -R "$SOURCE/examples" "$STAGE/examples"

if $CHECK; then
  if [ ! -d "$TARGET" ]; then
    echo "sync-contract: no existe contract/$VERSION/. Ejecuta scripts/sync-contract.sh." >&2
    exit 1
  fi
  if ! diff -r "$STAGE" "$TARGET" >&2; then
    echo "sync-contract: contract/$VERSION/ difiere de $SOURCE. Ejecuta scripts/sync-contract.sh." >&2
    exit 1
  fi
  echo "sync-contract: contract/$VERSION/ está al día."
  exit 0
fi

rm -rf "$TARGET"
mkdir -p "$(dirname "$TARGET")"
cp -R "$STAGE" "$TARGET"
echo "sync-contract: copiado $SOURCE → contract/$VERSION/."
