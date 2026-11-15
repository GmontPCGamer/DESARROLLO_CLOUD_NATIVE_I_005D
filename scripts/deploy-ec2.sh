#!/usr/bin/env bash
# Entrada compatible: usa la misma release y arquitectura que el despliegue SSM.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export INSTANCE_ID="${INSTANCE_ID:-i-0e3ea93394a16686d}"
export ARTIFACT_BUCKET="${ARTIFACT_BUCKET:-nexotech-jars-7717}"
exec bash "$ROOT/scripts/deploy-ssm.sh" "$@"
