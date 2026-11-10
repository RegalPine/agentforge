#!/usr/bin/env bash
# Generate SBOM (Software Bill of Materials) for AgentForge
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
OUTPUT="${PROJECT_DIR}/agentforge-sbom.json"

echo "Generating SBOM for AgentForge..."

cd "$PROJECT_DIR"

# Use Maven dependency:tree to generate a basic SBOM
./mvnw dependency:tree -DoutputType=json -DoutputFile="$OUTPUT" -B 2>/dev/null || {
  echo "Maven dependency:tree failed, generating fallback SBOM..."
  # Fallback: collect module info
  cat > "$OUTPUT" <<EOF
{
  "bomFormat": "AgentForge-SBOM",
  "specVersion": "1.0",
  "metadata": {
    "component": {
      "name": "agentforge",
      "version": "0.1.0",
      "type": "library"
    }
  },
  "modules": []
}
EOF
}

echo "SBOM written to: $OUTPUT"
