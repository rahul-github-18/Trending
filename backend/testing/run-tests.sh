#!/bin/bash
# Igniter Test Runner Script
# Usage:
#   ./testing/run-tests.sh all          (Runs all unit, integration, and workflow tests)
#   ./testing/run-tests.sh unit         (Runs fast Mockito unit tests)
#   ./testing/run-tests.sh integration  (Runs controller integration tests)
#   ./testing/run-tests.sh workflow     (Runs the complete End-to-End workflow test)

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

MODE="${1:-all}"

echo "=========================================="
echo " Running Igniter Test Suite - Mode: $MODE "
echo "=========================================="

case "$MODE" in
  unit)
    echo "Running Unit Tests..."
    ./mvnw test "-Dtest=*UnitTest"
    ;;
  integration)
    echo "Running Integration Tests..."
    ./mvnw test "-Dtest=*IntegrationTest,!EndToEndWorkflowIntegrationTest"
    ;;
  workflow)
    echo "Running End-to-End Workflow Integration Test..."
    ./mvnw test "-Dtest=EndToEndWorkflowIntegrationTest"
    ;;
  all)
    echo "Running All Tests (Unit + Integration + Workflow)..."
    ./mvnw test
    ;;
  *)
    echo "Unknown mode: $MODE"
    echo "Available modes: all, unit, integration, workflow"
    exit 1
    ;;
esac

echo "=========================================="
echo " All tests in mode '$MODE' completed successfully! "
echo "=========================================="
