# Disaster Recovery Plan

This document outlines procedures and strategies for disaster recovery in the event of deployment failures.

## Automatic Rollback

- The Jenkins pipeline includes health checks to verify green environment stability.
- If any health check fails, Jenkins automatically reroutes traffic back to the blue environment and scales it back up.
- Rollback is verified before marking the deployment as failed.

## Manual Rollback

- Use the `scripts/rollback.sh` script to manually revert to the previous stable environment.
- Example:  
    `./scripts/rollback.sh production blue`

- Verify rollback success with:
    kubectl get pods -l version=blue -n production
    kubectl get service myapp-service -o yaml -n production

## Monitoring and Alerts

- Continuous monitoring with Prometheus and Grafana.
- Alert on failed deployments or degraded pod health.
- Periodic reviews and updates of disaster recovery procedures.

Please refer to the main [README.md](../README.md) for additional context.

