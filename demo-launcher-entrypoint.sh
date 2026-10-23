#!/usr/bin/env bash
# bootstrap.sh - Debian/Ubuntu. Run as root.
set -euo pipefail

# ---- config ----
ARTIFACT_URL="https://github.com/dreamoutbox/java-apartment-management-system/releases/latest/download/ams-1.3.0.jar"
JAVA_OPTS_DEFAULT="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# ----------------

[[ $EUID -eq 0 ]] || { echo "run as root" >&2; exit 1; }

# derive <repo> from URL
re='^https://github\.com/([^/]+)/([^/]+)/releases/(latest/download|download/[^/]+)/[^/]+$'
[[ "$ARTIFACT_URL" =~ $re ]] || { echo "unexpected ARTIFACT_URL format" >&2; exit 1; }
APP_NAME="${APP_NAME:-${BASH_REMATCH[2]}}"
APP_USER="${APP_USER:-$APP_NAME}"
APP_DIR="/app/${APP_NAME}"
DATA_DIR="${APP_DIR}/data"
JAR_PATH="${APP_DIR}/${APP_NAME}.jar"
ENV_FILE="/etc/${APP_NAME}.env"

export DEBIAN_FRONTEND=noninteractive

# --- packages ---
apt-get update -y
apt-get install -y --no-install-recommends \
    git \
    curl \
    ca-certificates \
    gnupg \
    apt-transport-https

# --- Temurin 25 (Adoptium apt repo) ---
if ! dpkg -s temurin-25-jdk >/dev/null 2>&1; then
  install -d -m 0755 /etc/apt/keyrings
  curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
    | gpg --dearmor --yes -o /etc/apt/keyrings/adoptium.gpg
  . /etc/os-release
  echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" \
    > /etc/apt/sources.list.d/adoptium.list
  apt-get update -y
  apt-get install -y temurin-25-jdk
fi

# --- create service user ---
if ! id -u "$APP_USER" >/dev/null 2>&1; then
  useradd --system --no-create-home --home-dir "$APP_DIR" --shell /usr/sbin/nologin "$APP_USER"
fi
# root-owned dir/jar: service can read/execute but not overwrite its own binary
install -d -o root -g "$APP_USER" -m 0750 "$APP_DIR"

# --- download jar (atomic replace) ---
tmp=$(mktemp "${APP_DIR}/.download.XXXXXX")
trap 'rm -f "$tmp"' EXIT
curl -fsSL --retry 3 --retry-all-errors -o "$tmp" "$ARTIFACT_URL"
[[ "$(head -c2 "$tmp")" == "PK" ]] || { echo "downloaded file is not a jar/zip" >&2; exit 1; }
chown root:"$APP_USER" "$tmp"
chmod 0440 "$tmp"
mv -f "$tmp" "$JAR_PATH"
trap - EXIT

# --- env file for secrets/overrides ---
if [[ ! -f "$ENV_FILE" ]]; then
  cat > "$ENV_FILE" <<EOF
JAVA_OPTS="${JAVA_OPTS_DEFAULT}"
# SPRING_DATASOURCE_URL=
# SPRING_DATASOURCE_PASSWORD=
EOF
  chown root:"$APP_USER" "$ENV_FILE"
  chmod 0640 "$ENV_FILE"
fi

# --- create data directory ---
# writable by the service user only
install -d -o "$APP_USER" -g "$APP_USER" -m 0750 "$DATA_DIR"

# --- systemd unit ---
cat > "/etc/systemd/system/${APP_NAME}.service" <<EOF
[Unit]
Description=${APP_NAME} (Spring Boot)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ReadWritePaths=${DATA_DIR}
ExecStart=/usr/bin/java \$JAVA_OPTS -Dspring.profiles.active=prod -jar ${JAR_PATH}
SuccessExitStatus=143
Restart=always
RestartSec=5
TimeoutStopSec=30

NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ProtectKernelTunables=true
ProtectControlGroups=true
RestrictSUIDSGID=true
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "${APP_NAME}.service"
systemctl restart "${APP_NAME}.service"

systemctl --no-pager --full status "${APP_NAME}.service" || true

echo "logs: journalctl -u ${APP_NAME} -f"
