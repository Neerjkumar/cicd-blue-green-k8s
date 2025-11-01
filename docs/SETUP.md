# Setup Instructions

This document guides you through setting up the CI/CD pipeline environment.

## Prerequisites

- Jenkins server (version 2.300+)
- Kubernetes cluster (version 1.20+)
- kubectl CLI configured for your cluster
- Docker installed for building images
- Jenkins plugins installed:
  - Git Plugin
  - Kubernetes Plugin
  - Pipeline Plugin
  - Blue Ocean (optional)

## Steps

1. Clone this repository.
2. Configure Jenkins credentials for:
   - Git repository access
   - Docker registry login
   - Kubernetes access (kubeconfig)
3. Deploy Jenkins to Kubernetes using `jenkins/jenkins-deployment.yaml`.
4. Deploy Kubernetes manifests in the `kubernetes/` directory.
5. Create Jenkins pipeline job pointing to this repo’s `Jenkinsfile.groovy`.
6. Set Git webhook for Jenkins to trigger builds on commit.
7. Test deployment by making a sample commit.

More detailed instructions and troubleshooting at [README.md](../README.md).
