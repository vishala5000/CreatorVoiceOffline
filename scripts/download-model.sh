#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS="$ROOT/app/src/main/assets"
MODEL="kokoro-en-v0_19"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/${MODEL}.tar.bz2"

mkdir -p "$ASSETS"
cd "$ASSETS"

if [[ -d "$MODEL" && -f "$MODEL/model.onnx" && -f "$MODEL/voices.bin" ]]; then
  echo "Model already present."
  exit 0
fi

rm -rf "$MODEL" "${MODEL}.tar.bz2"
curl -fL --retry 4 --retry-delay 2 -o "${MODEL}.tar.bz2" "$URL"
tar -xjf "${MODEL}.tar.bz2"
rm -f "${MODEL}.tar.bz2"

test -f "$MODEL/model.onnx"
test -f "$MODEL/voices.bin"
test -f "$MODEL/tokens.txt"

echo "Installed $MODEL"
du -sh "$MODEL"
