#!/bin/bash
COLOR=${1:-green}
NAMESPACE=${2:-production}

echo "Checking health for $COLOR environment pods..."
kubectl get pods -l app=myapp,version=$COLOR -n $NAMESPACE

# Optional: Add custom health validation logic below
