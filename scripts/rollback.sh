#!/bin/bash
NAMESPACE=${1:-production}
PREVIOUS_COLOR=${2:-blue}

echo "Rolling back to $PREVIOUS_COLOR environment in namespace $NAMESPACE..."

kubectl patch service myapp-service -n $NAMESPACE \
  -p "{\"spec\":{\"selector\":{\"version\":\"$PREVIOUS_COLOR\"}}}"

kubectl scale deployment/myapp-$PREVIOUS_COLOR --replicas=3 -n $NAMESPACE
