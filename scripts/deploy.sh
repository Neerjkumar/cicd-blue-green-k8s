#!/bin/bash
# Script to deploy application to the green environment
echo "Deploying application to green environment..."
kubectl apply -f kubernetes/green-deployment.yaml
kubectl rollout status deployment/myapp-green -n production
