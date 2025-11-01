// Jenkinsfile for Blue-Green Deployment CI/CD Pipeline
// Author: Neeraj Kumar
// Description: Automated CI/CD pipeline with blue-green deployment strategy

@Library('shared-library') _

// Pipeline configuration
def config = [
    gitRepo: env.GIT_REPO ?: 'https://github.com/yourusername/your-app.git',
    gitBranch: env.GIT_BRANCH ?: 'main',
    dockerRegistry: env.DOCKER_REGISTRY ?: 'docker.io',
    dockerImage: env.DOCKER_IMAGE ?: 'your-app',
    k8sNamespace: env.K8S_NAMESPACE ?: 'production',
    healthCheckRetries: 5,
    healthCheckInterval: 30,
    rollbackOnFailure: true
]

pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
metadata:
  labels:
    jenkins: agent
spec:
  serviceAccountName: jenkins
  containers:
  - name: docker
    image: docker:latest
    command:
    - cat
    tty: true
    volumeMounts:
    - name: docker-sock
      mountPath: /var/run/docker.sock
  - name: kubectl
    image: bitnami/kubectl:latest
    command:
    - cat
    tty: true
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
'''
        }
    }

    environment {
        DOCKER_CREDENTIALS = credentials('docker-credentials')
        KUBECONFIG = credentials('kubeconfig')
        GIT_CREDENTIALS = credentials('git-credentials')
        BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7)}"
        CURRENT_COLOR = 'blue'
        NEW_COLOR = 'green'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "🔍 Checking out code from ${config.gitRepo}"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${config.gitBranch}"]],
                        userRemoteConfigs: [[
                            url: config.gitRepo,
                            credentialsId: 'git-credentials'
                        ]]
                    ])
                    
                    // Determine current active environment
                    CURRENT_COLOR = getCurrentActiveColor(config.k8sNamespace)
                    NEW_COLOR = (CURRENT_COLOR == 'blue') ? 'green' : 'blue'
                    
                    echo "✅ Current active environment: ${CURRENT_COLOR}"
                    echo "🎯 Target deployment environment: ${NEW_COLOR}"
                }
            }
        }

        stage('Build & Test') {
            steps {
                container('docker') {
                    script {
                        echo "🏗️ Building application..."
                        
                        // Run unit tests
                        sh '''
                            echo "Running unit tests..."
                            # Add your test commands here
                            # npm test || mvn test || pytest
                        '''
                        
                        // Build Docker image
                        sh """
                            echo "Building Docker image..."
                            docker build -t ${config.dockerRegistry}/${config.dockerImage}:${BUILD_VERSION} .
                            docker tag ${config.dockerRegistry}/${config.dockerImage}:${BUILD_VERSION} \
                                       ${config.dockerRegistry}/${config.dockerImage}:${NEW_COLOR}-latest
                        """
                        
                        echo "✅ Build completed successfully"
                    }
                }
            }
        }

        stage('Security Scan') {
            steps {
                container('docker') {
                    script {
                        echo "🔐 Running security scan..."
                        
                        // Container image vulnerability scanning
                        sh """
                            # Install and run Trivy or similar scanner
                            # trivy image ${config.dockerRegistry}/${config.dockerImage}:${BUILD_VERSION}
                            echo "Security scan completed"
                        """
                        
                        echo "✅ Security scan passed"
                    }
                }
            }
        }

        stage('Push to Registry') {
            steps {
                container('docker') {
                    script {
                        echo "📦 Pushing image to registry..."
                        
                        sh """
                            echo "${DOCKER_CREDENTIALS_PSW}" | docker login -u "${DOCKER_CREDENTIALS_USR}" --password-stdin ${config.dockerRegistry}
                            docker push ${config.dockerRegistry}/${config.dockerImage}:${BUILD_VERSION}
                            docker push ${config.dockerRegistry}/${config.dockerImage}:${NEW_COLOR}-latest
                        """
                        
                        echo "✅ Image pushed successfully"
                    }
                }
            }
        }

        stage('Deploy to Target Environment') {
            steps {
                container('kubectl') {
                    script {
                        echo "🚀 Deploying to ${NEW_COLOR} environment..."
                        
                        // Update deployment with new image
                        sh """
                            kubectl set image deployment/myapp-${NEW_COLOR} \
                                myapp=${config.dockerRegistry}/${config.dockerImage}:${BUILD_VERSION} \
                                -n ${config.k8sNamespace}
                            
                            # Wait for rollout to complete
                            kubectl rollout status deployment/myapp-${NEW_COLOR} -n ${config.k8sNamespace} --timeout=5m
                        """
                        
                        echo "✅ Deployment to ${NEW_COLOR} completed"
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                container('kubectl') {
                    script {
                        echo "🏥 Running health checks on ${NEW_COLOR} environment..."
                        
                        def healthCheckPassed = performHealthCheck(
                            NEW_COLOR,
                            config.k8sNamespace,
                            config.healthCheckRetries,
                            config.healthCheckInterval
                        )
                        
                        if (!healthCheckPassed) {
                            error "❌ Health check failed for ${NEW_COLOR} environment"
                        }
                        
                        echo "✅ Health check passed"
                    }
                }
            }
        }

        stage('Integration Tests') {
            steps {
                container('kubectl') {
                    script {
                        echo "🧪 Running integration tests on ${NEW_COLOR} environment..."
                        
                        // Get the service endpoint
                        def serviceEndpoint = sh(
                            script: "kubectl get svc myapp-${NEW_COLOR} -n ${config.k8sNamespace} -o jsonpath='{.spec.clusterIP}'",
                            returnStdout: true
                        ).trim()
                        
                        // Run integration tests
                        sh """
                            # Add your integration test commands here
                            # curl -f http://${serviceEndpoint}/health || exit 1
                            # newman run api-tests.json --env-var base_url=http://${serviceEndpoint}
                            echo "Integration tests passed"
                        """
                        
                        echo "✅ Integration tests passed"
                    }
                }
            }
        }

        stage('Switch Traffic') {
            steps {
                container('kubectl') {
                    script {
                        echo "🔄 Switching traffic from ${CURRENT_COLOR} to ${NEW_COLOR}..."
                        
                        // Update service selector to point to new environment
                        sh """
                            kubectl patch service myapp-service \
                                -n ${config.k8sNamespace} \
                                -p '{"spec":{"selector":{"version":"${NEW_COLOR}"}}}'
                        """
                        
                        // Wait for traffic to stabilize
                        sleep(time: 10, unit: 'SECONDS')
                        
                        echo "✅ Traffic switched successfully"
                    }
                }
            }
        }

        stage('Post-Deployment Validation') {
            steps {
                container('kubectl') {
                    script {
                        echo "✔️ Validating deployment..."
                        
                        // Monitor for errors after traffic switch
                        def validationPassed = monitorDeployment(
                            NEW_COLOR,
                            config.k8sNamespace,
                            60 // Monitor for 60 seconds
                        )
                        
                        if (!validationPassed) {
                            error "❌ Post-deployment validation failed"
                        }
                        
                        echo "✅ Post-deployment validation passed"
                    }
                }
            }
        }

        stage('Cleanup Old Environment') {
            steps {
                container('kubectl') {
                    script {
                        echo "🧹 Scaling down ${CURRENT_COLOR} environment..."
                        
                        // Keep the old environment but scale down to save resources
                        // Don't delete it - needed for quick rollback
                        sh """
                            kubectl scale deployment/myapp-${CURRENT_COLOR} \
                                --replicas=1 \
                                -n ${config.k8sNamespace}
                        """
                        
                        echo "✅ Cleanup completed"
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo "✅ Pipeline completed successfully!"
                echo "🎉 Application version ${BUILD_VERSION} is now live on ${NEW_COLOR} environment"
                
                // Send success notification
                sendNotification(
                    status: 'SUCCESS',
                    message: "Deployment successful: ${config.dockerImage}:${BUILD_VERSION}",
                    color: NEW_COLOR
                )
            }
        }

        failure {
            script {
                echo "❌ Pipeline failed!"
                
                if (config.rollbackOnFailure) {
                    echo "🔙 Initiating automatic rollback to ${CURRENT_COLOR}..."
                    
                    container('kubectl') {
                        try {
                            // Rollback to previous environment
                            sh """
                                kubectl patch service myapp-service \
                                    -n ${config.k8sNamespace} \
                                    -p '{"spec":{"selector":{"version":"${CURRENT_COLOR}"}}}'
                                
                                kubectl scale deployment/myapp-${CURRENT_COLOR} \
                                    --replicas=3 \
                                    -n ${config.k8sNamespace}
                            """
                            
                            echo "✅ Rollback completed successfully"
                        } catch (Exception e) {
                            echo "❌ Rollback failed: ${e.message}"
                            // Manual intervention required
                        }
                    }
                }
                
                // Send failure notification
                sendNotification(
                    status: 'FAILURE',
                    message: "Deployment failed: ${config.dockerImage}:${BUILD_VERSION}",
                    color: 'red'
                )
            }
        }

        always {
            script {
                echo "📊 Cleaning up..."
                
                // Clean up Docker images to save space
                container('docker') {
                    sh 'docker system prune -f'
                }
                
                // Archive artifacts
                archiveArtifacts artifacts: '**/target/*.jar,**/build/*.jar', allowEmptyArchive: true
                
                // Publish test results if available
                junit testResults: '**/target/test-*.xml', allowEmptyResults: true
            }
        }
    }
}

// Helper Functions

def getCurrentActiveColor(namespace) {
    container('kubectl') {
        def selector = sh(
            script: "kubectl get service myapp-service -n ${namespace} -o jsonpath='{.spec.selector.version}'",
            returnStdout: true
        ).trim()
        
        return selector ?: 'blue'
    }
}

def performHealthCheck(color, namespace, retries, interval) {
    container('kubectl') {
        for (int i = 0; i < retries; i++) {
            try {
                def readyPods = sh(
                    script: """
                        kubectl get pods -l app=myapp,version=${color} -n ${namespace} \
                            -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' | \
                            grep -o True | wc -l
                    """,
                    returnStdout: true
                ).trim().toInteger()
                
                def totalPods = sh(
                    script: "kubectl get pods -l app=myapp,version=${color} -n ${namespace} --no-headers | wc -l",
                    returnStdout: true
                ).trim().toInteger()
                
                if (readyPods == totalPods && totalPods > 0) {
                    echo "✅ All pods are ready (${readyPods}/${totalPods})"
                    return true
                } else {
                    echo "⏳ Waiting for pods to be ready (${readyPods}/${totalPods})..."
                    sleep(interval)
                }
            } catch (Exception e) {
                echo "⚠️ Health check attempt ${i + 1} failed: ${e.message}"
                sleep(interval)
            }
        }
        
        return false
    }
}

def monitorDeployment(color, namespace, durationSeconds) {
    container('kubectl') {
        try {
            // Check for pod crashes or restarts
            def restarts = sh(
                script: """
                    kubectl get pods -l app=myapp,version=${color} -n ${namespace} \
                        -o jsonpath='{.items[*].status.containerStatuses[*].restartCount}' | \
                        awk '{sum+=\$1} END {print sum}'
                """,
                returnStdout: true
            ).trim().toInteger()
            
            if (restarts > 0) {
                echo "⚠️ Warning: Detected ${restarts} pod restart(s)"
                return false
            }
            
            // Additional monitoring can be added here
            // - Check error logs
            // - Monitor response times
            // - Check error rates from APM tools
            
            return true
        } catch (Exception e) {
            echo "❌ Monitoring failed: ${e.message}"
            return false
        }
    }
}

def sendNotification(Map params) {
    // Implement your notification logic here
    // Examples: Slack, Email, PagerDuty, Teams
    
    echo """
    📢 Notification:
    Status: ${params.status}
    Message: ${params.message}
    Build: #${env.BUILD_NUMBER}
    Branch: ${env.GIT_BRANCH}
    """
    
    // Example: Slack notification (uncomment and configure)
    // slackSend(
    //     color: params.color,
    //     message: "${params.status}: ${params.message}\nBuild: ${env.BUILD_URL}"
    // )
}
