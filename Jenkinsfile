// Declarative pipeline: build and test everything, then (on main only) publish images and deploy.
//
// Jenkins prerequisites
//   Tools (Manage Jenkins > Tools):  jdk "jdk-21", maven "maven-3.9", nodejs "node-22"
//   Agent:                           Docker available (Testcontainers and image builds need it)
//   Credentials:                     "registry-credentials"  (username/password for the image registry)
//                                    "deploy-ssh-key"        (SSH private key for the deploy host)
//   Environment (job or folder):     REGISTRY (e.g. ghcr.io/your-user), DEPLOY_HOST (e.g. ubuntu@1.2.3.4)

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    tools {
        jdk 'jdk-21'
        maven 'maven-3.9'
        nodejs 'node-22'
    }

    environment {
        // Short commit hash makes every image traceable to the exact source.
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'local'}"
    }

    stages {
        stage('Build and test') {
            parallel {
                stage('API') {
                    steps {
                        dir('backend') {
                            // verify = compile + unit tests + Testcontainers integration tests
                            sh 'mvn -B verify'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Notification service') {
                    steps {
                        dir('notification-service') {
                            sh 'mvn -B verify'
                        }
                    }
                }
                stage('Web') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci --no-audit --no-fund'
                            sh 'npm run build' // type-check + production bundle
                        }
                    }
                }
            }
        }

        stage('Build images') {
            when { branch 'main' }
            steps {
                sh '''
                    docker build -t "$REGISTRY/seatsure-api:$IMAGE_TAG"          backend
                    docker build -t "$REGISTRY/seatsure-notification:$IMAGE_TAG" notification-service
                    docker build -t "$REGISTRY/seatsure-web:$IMAGE_TAG"          frontend
                '''
            }
        }

        stage('Push images') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'registry-credentials',
                                                  usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                    sh '''
                        echo "$REG_PASS" | docker login "${REGISTRY%%/*}" -u "$REG_USER" --password-stdin
                        for image in api notification web; do
                            docker push "$REGISTRY/seatsure-$image:$IMAGE_TAG"
                        done
                    '''
                }
            }
        }

        stage('Deploy') {
            when { branch 'main' }
            steps {
                // Assumes the host already has docker-compose.yml and .env in /opt/seatsure.
                sshagent(credentials: ['deploy-ssh-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=accept-new "$DEPLOY_HOST" \
                          "cd /opt/seatsure && export REGISTRY=$REGISTRY TAG=$IMAGE_TAG \
                           && docker compose pull api notification web \
                           && docker compose up -d --no-build"
                    '''
                }
            }
        }

        stage('Smoke test') {
            when { branch 'main' }
            steps {
                // Fails the build (and flags a bad deploy) if the API is not healthy after rollout.
                sh '''
                    for attempt in $(seq 1 12); do
                        if curl -fsS "http://${DEPLOY_HOST#*@}:8080/actuator/health/readiness" | grep -q UP; then
                            echo "API is healthy"; exit 0
                        fi
                        sleep 5
                    done
                    echo "API did not become healthy in time"; exit 1
                '''
            }
        }
    }

    post {
        failure {
            echo 'Build failed. Check the stage logs above; test reports are attached to the build.'
        }
        cleanup {
            cleanWs()
        }
    }
}
