# CI/CD Pipeline with Blue-Green Deployment on Kubernetes

[![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-red?logo=jenkins)](https://www.jenkins.io/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Orchestration-blue?logo=kubernetes)](https://kubernetes.io/)
[![Groovy](https://img.shields.io/badge/Groovy-Scripting-orange?logo=apache-groovy)](https://groovy-lang.org/)

A production-ready CI/CD pipeline implementation featuring automated builds, blue-green deployment strategy, and comprehensive disaster recovery mechanisms for Kubernetes environments.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Repository Structure](#repository-structure)
- [Detailed Setup](#detailed-setup)
- [Blue-Green Deployment](#blue-green-deployment)
- [Disaster Recovery](#disaster-recovery)
- [Monitoring & Logging](#monitoring--logging)
- [Testing](#testing)
- [Security Best Practices](#security-best-practices)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

This project implements a complete CI/CD pipeline that:
- Automatically builds artifacts on Git commits
- Deploys applications to Kubernetes clusters
- Implements zero-downtime blue-green deployments
- Provides automatic rollback on deployment failures
- Ensures high availability and disaster recovery

## ✨ Features

- **Automated CI/CD**: Trigger builds automatically on Git commits
- **Blue-Green Deployment**: Zero-downtime deployments with instant rollback
- **Disaster Recovery**: Automatic failure detection and rollback mechanisms
- **Scalability**: Support for multiple Git repositories and Kubernetes clusters
- **Monitoring**: Built-in health checks and deployment validation
- **Security**: Implements security best practices throughout the pipeline
- **Extensibility**: Groovy-based flexible automation scripts

## 🏗 Architecture

```
┌─────────────┐     ┌──────────────┐     ┌───────────────────┐
│   Git Repo  │────▶│   Jenkins    │────▶│   Kubernetes      │
│   (Commit)  │     │   Pipeline   │     │   Cluster         │
└─────────────┘     └──────────────┘     └───────────────────┘
                           │                      │
                           │                      ▼
                           │              ┌───────────────┐
                           │              │  Blue Env     │
                           │              │  (Production) │
                           │              └───────────────┘
                           │                      │
                           │                      ▼
                           │              ┌───────────────┐
                           └─────────────▶│  Green Env    │
                                          │  (Staging)    │
                                          └───────────────┘
```

## 📋 Prerequisites

- **Git**: Version 2.x or higher
- **Jenkins**: Version 2.300+ with the following plugins:
  - Git Plugin
  - Kubernetes Plugin
  - Pipeline Plugin
  - Blue Ocean (optional, for better visualization)
- **Kubernetes Cluster**: Version 1.20+
  - kubectl configured with cluster access
  - Minimum 2 worker nodes recommended
- **Docker**: For building container images
- **Groovy**: 2.5+ (bundled with Jenkins)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/cicd-blue-green-k8s.git
cd cicd-blue-green-k8s
```

### 2. Deploy Jenkins to Kubernetes (Optional)

If running Jenkins in Kubernetes:

```bash
# Create Jenkins namespace
kubectl create namespace jenkins

# Deploy Jenkins
kubectl apply -f jenkins/jenkins-deployment.yaml

# Get Jenkins admin password
kubectl exec -n jenkins -it deployment/jenkins -- cat /var/jenkins_home/secrets/initialAdminPassword
```

### 3. Configure Jenkins

```bash
# Create Jenkins credentials for Git and Kubernetes
# Navigate to: Jenkins → Manage Jenkins → Manage Credentials

# Add the following credentials:
# - Git credentials (ID: git-credentials)
# - Kubernetes config (ID: kubeconfig)
# - Docker registry credentials (ID: docker-credentials)
```

### 4. Create Jenkins Pipeline

1. Create a new Pipeline job in Jenkins
2. Point to your Git repository
3. Select `Jenkinsfile.groovy` as the pipeline script path
4. Configure webhook for automatic triggers

### 5. Deploy Application to Kubernetes

```bash
# Create production namespace
kubectl create namespace production

# Apply Kubernetes manifests
kubectl apply -f kubernetes/blue-deployment.yaml
kubectl apply -f kubernetes/green-deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml
```

### 6. Trigger Your First Build

```bash
# Make a commit to trigger the pipeline
git add .
git commit -m "Initial deployment"
git push origin main
```

## 📁 Repository Structure

```
.
├── README.md
├── Jenkinsfile.groovy                 # Main pipeline definition
├── .gitignore                         # Git ignore patterns
│
├── jenkins/
│   └── jenkins-deployment.yaml        # Jenkins K8s deployment (PVC, Deployment, Service)
│
├── kubernetes/
│   ├── blue-deployment.yaml           # Blue environment deployment
│   ├── green-deployment.yaml          # Green environment deployment
│   ├── service.yaml                   # Kubernetes service configuration
│   └── ingress.yaml                   # Ingress controller configuration
│
├── scripts/
│   ├── deploy.sh                      # Deployment automation script
│   ├── rollback.sh                    # Rollback automation script
│   └── health-check.sh                # Health check validation script
│
└── docs/
    ├── SETUP.md                       # Detailed setup instructions
    ├── DISASTER_RECOVERY.md           # DR procedures and guidelines
    └── TESTING.md                     # Testing procedures and test cases
```

## 🔧 Detailed Setup

For detailed setup instructions, please refer to [docs/SETUP.md](docs/SETUP.md).

Key configuration steps:
1. Jenkins configuration and plugin setup
2. Kubernetes cluster preparation
3. Git webhook configuration
4. Environment variables and secrets management
5. Network and ingress setup

## 🔄 Blue-Green Deployment

### How It Works

1. **Blue Environment**: Currently serving production traffic
2. **Green Environment**: New version deployed here
3. **Health Checks**: Validate green environment health
4. **Traffic Switch**: If healthy, switch traffic to green
5. **Blue Becomes Standby**: Blue environment remains for instant rollback

### Deployment Flow

```groovy
stage('Deploy to Green') {
    // Deploy new version to green environment
}

stage('Health Check') {
    // Validate green environment
}

stage('Switch Traffic') {
    // Route production traffic to green
}

stage('Monitor') {
    // Watch for issues, ready to rollback
}
```

### Traffic Switching

Traffic is switched by updating the Kubernetes service selector:

```yaml
selector:
  app: myapp
  version: green  # Changed from 'blue' to 'green'
```

## 🚨 Disaster Recovery

### Automatic Rollback Triggers

- Failed health checks in green environment
- Deployment timeout (configurable)
- Post-deployment error rate threshold exceeded
- Manual rollback trigger

### Rollback Procedure

```bash
# Automatic rollback (handled by pipeline)
# Or manual rollback:
./scripts/rollback.sh production blue

# Verify rollback
kubectl get pods -l version=blue -n production
kubectl get service myapp-service -o yaml -n production
```

For comprehensive disaster recovery documentation, see [docs/DISASTER_RECOVERY.md](docs/DISASTER_RECOVERY.md).

## 📊 Monitoring & Logging

### Recommended Monitoring Stack

- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **ELK/EFK Stack**: Centralized logging
- **Jaeger/Zipkin**: Distributed tracing

### Key Metrics to Monitor

- Pod health and readiness
- Request success/error rates
- Response time and latency
- Resource utilization (CPU, Memory)
- Deployment success/failure rates

### Logging Strategy

```bash
# View Jenkins logs
kubectl logs -f deployment/jenkins -n jenkins

# View application logs (blue environment)
kubectl logs -f deployment/myapp-blue -n production

# View application logs (green environment)
kubectl logs -f deployment/myapp-green -n production
```

## 🧪 Testing

### Test Cases Included

1. **Unit Tests**: Pre-deployment code validation
2. **Integration Tests**: API and service integration validation
3. **Health Check Tests**: Endpoint availability validation
4. **Load Tests**: Performance under expected traffic
5. **Rollback Tests**: Disaster recovery validation

### Running Tests

```bash
# Validate deployment
./scripts/health-check.sh green
```

For detailed testing procedures, see [docs/TESTING.md](docs/TESTING.md).

## 🔐 Security Best Practices

### Implemented Security Measures

- **Secrets Management**: Use Kubernetes Secrets for sensitive data
- **RBAC**: Role-based access control for Kubernetes resources
- **Network Policies**: Restrict pod-to-pod communication
- **Image Scanning**: Container vulnerability scanning
- **TLS/SSL**: Encrypted communication via Ingress
- **Least Privilege**: Minimal permissions for service accounts

### Security Checklist

- ✅ Secrets stored securely (not in Git)
- ✅ Container images scanned for vulnerabilities
- ✅ Network policies implemented
- ✅ RBAC configured with minimal permissions
- ✅ Ingress configured with TLS certificates
- ✅ Jenkins credentials properly encrypted

## 🔍 Troubleshooting

### Common Issues

**Issue**: Pipeline fails at build stage
```bash
# Check Jenkins logs
kubectl logs -n jenkins deployment/jenkins

# Verify Git credentials
kubectl get secret git-credentials -n jenkins
```

**Issue**: Deployment stuck in pending state
```bash
# Check pod status
kubectl describe pod <pod-name>

# Check node resources
kubectl top nodes
```

**Issue**: Health checks failing
```bash
# Check pod logs
kubectl logs deployment/myapp-green -n production

# Verify service endpoints
kubectl get endpoints myapp-service -n production
```

**Issue**: Traffic not switching to green
```bash
# Verify service selector
kubectl get service myapp-service -o yaml -n production

# Check ingress configuration
kubectl describe ingress myapp-ingress -n production
```

### Debug Commands

```bash
# Check deployment status
kubectl rollout status deployment/myapp-green -n production

# View recent events
kubectl get events --sort-by=.metadata.creationTimestamp -n production

# Test service connectivity
kubectl run test-pod --image=curlimages/curl --rm -it -- curl http://myapp-service
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Neeraj Kumar**
- LinkedIn: [Neeraj Kumar](https://www.linkedin.com/in/neeraj529kumar/)
- Email: neerajnirala1999@gmail.com
- Location: Mumbai, Maharashtra

## 🙏 Acknowledgments

- Jenkins community for excellent documentation
- Kubernetes community for container orchestration
- DevOps best practices from industry leaders

## 📞 Support

For issues, questions, or suggestions:
- Open an issue in this repository
- Contact via email: neerajnirala1999@gmail.com
- Connect on LinkedIn

---

**Note**: This is a demonstration project for educational and portfolio purposes. Adapt configurations according to your specific production requirements and security policies.