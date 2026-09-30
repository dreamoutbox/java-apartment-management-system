#!/bin/sh
set -e

IMAGE_TAG="${1:-latest}"
IMAGE_NAME="ams:${IMAGE_TAG}"

echo "Building Docker image ${IMAGE_NAME}..."
docker build -t "${IMAGE_NAME}" .

echo "Docker image ${IMAGE_NAME} built successfully."
