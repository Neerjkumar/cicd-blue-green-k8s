# Testing Procedures

This document describes the testing strategy to validate the CI/CD pipeline and deployments.

## Test Cases Included

1. **Unit Tests** - Run during the build stage to verify code correctness.
2. **Integration Tests** - Validate service interactions and API endpoints post-deployment.
3. **Health Check Tests** - Confirm pod readiness and liveness using endpoint validation.
4. **Load Tests** - Assess performance under expected traffic volume (optional).
5. **Rollback Tests** - Simulate deployment failures and verify automatic/manual rollback.

## Running Tests

- Execute unit and integration tests via commands integrated in the Jenkinsfile build stage.
- Use the health check script to verify pod status:
    `./scripts/health-check.sh green`

- For rollback tests, deliberately trigger a failing deployment and observe rollback procedures.

## Additional Recommendations

- Automate running test cases via Jenkins pipeline stages for continuous validation.
- Maintain up-to-date test cases synchronized with application changes.
- Monitor Jenkins and Kubernetes logs for test results and anomalies.

See [README.md](../README.md) for more context.
