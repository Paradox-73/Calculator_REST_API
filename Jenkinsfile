// Declarative CI/CD pipeline for the Calculator REST API.
// Stages: Checkout -> Build -> Test -> Docker Build -> Push -> Deploy.
//
// CSE 816 Software Production Engineering - Mini-Project
// Kanav Bhardwaj (IMT2023024)

/** Runs a shell command on a Linux agent or on a Windows agent. */
def runCmd(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

pipeline {

    agent any

    tools {
        // Configure these names in "Manage Jenkins > Tools".
        maven 'Maven3'
        jdk 'JDK21'
    }

    environment {
        // TODO: set DOCKER_NAMESPACE to your real Docker Hub account name.
        DOCKER_NAMESPACE = 'kanavbhardwaj'
        IMAGE_REPO       = 'calculator-devops'
        IMAGE_NAME       = "${DOCKER_NAMESPACE}/${IMAGE_REPO}"
        IMAGE_TAG        = "${env.BUILD_NUMBER}"
        REGISTRY_CREDS   = 'dockerhub-credentials'   // Jenkins credentials ID
        // TODO: set NOTIFY_EMAIL to the address that must receive the reports.
        NOTIFY_EMAIL     = 'kanav.bhardwaj@iiitb.ac.in'
        // Jenkins itself uses port 8080, so the stack uses port 8081.
        HOST_PORT        = '8081'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        // One deployment at a time keeps the running stack consistent.
        disableConcurrentBuilds()
    }

    triggers {
        // The GitHub webhook starts the build. Polling is the fallback.
        githubPush()
        pollSCM('H/5 * * * *')
    }

    stages {

        stage('Checkout') {
            steps {
                echo "Build #${env.BUILD_NUMBER} of ${env.JOB_NAME}"
                checkout scm
                script {
                    env.GIT_SHORT = (isUnix()
                            ? sh(script: 'git rev-parse --short HEAD', returnStdout: true)
                            : bat(script: '@git rev-parse --short HEAD', returnStdout: true)).trim()
                }
                echo "Checked out commit ${env.GIT_SHORT}"
            }
        }

        stage('Build') {
            steps {
                echo 'Compiling the application and packaging the JAR.'
                runCmd 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            steps {
                echo 'Running the JUnit 5 unit tests and the MockMvc integration tests.'
                runCmd 'mvn -B test'
            }
            post {
                always {
                    // The build fails if a test fails.
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: false,
                          skipPublishingChecks: true
                    archiveArtifacts artifacts: 'target/site/jacoco/**',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "Building image ${IMAGE_NAME}:${IMAGE_TAG}"
                runCmd "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
                runCmd "docker image inspect ${IMAGE_NAME}:${IMAGE_TAG} --format \"Image size: {{.Size}} bytes\""
            }
        }

        stage('Push') {
            steps {
                echo "Pushing ${IMAGE_NAME} to Docker Hub."
                // The credentials never appear in the Jenkinsfile or in the log.
                withCredentials([usernamePassword(credentialsId: "${REGISTRY_CREDS}",
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        if (isUnix()) {
                            sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                        } else {
                            bat 'docker login -u %DOCKER_USER% -p %DOCKER_PASS%'
                        }
                    }
                    runCmd "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                    runCmd "docker push ${IMAGE_NAME}:latest"
                }
            }
            post {
                always {
                    // Remove the local credential file after the push.
                    runCmd 'docker logout'
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying ${IMAGE_NAME}:${IMAGE_TAG} with Docker Compose."
                withEnv(["IMAGE_NAME=${IMAGE_NAME}", "IMAGE_TAG=${IMAGE_TAG}", "HOST_PORT=${HOST_PORT}"]) {
                    // "up -d" replaces only the changed containers. A re-run
                    // of the same build does not break the running stack.
                    runCmd 'docker compose up -d --remove-orphans'
                    runCmd 'docker compose ps'
                }
            }
        }

        stage('Verify Health') {
            steps {
                echo 'Waiting for the stack to report a healthy state.'
                retry(12) {
                    sleep time: 5, unit: 'SECONDS'
                    runCmd "curl -sf http://localhost:${HOST_PORT}/actuator/health"
                }
                echo 'Running a smoke test through the Nginx proxy.'
                runCmd "curl -sf \"http://localhost:${HOST_PORT}/api/v1/add?a=2&b=3\""
            }
        }
    }

    post {

        always {
            echo 'Removing dangling images to save disk space.'
            script {
                try {
                    runCmd 'docker image prune -f'
                } catch (Exception e) {
                    echo "Image cleanup skipped: ${e.message}"
                }
            }
        }

        success {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "SUCCESS - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """
                    <h3>Build succeeded</h3>
                    <ul>
                      <li><b>Job:</b> ${env.JOB_NAME}</li>
                      <li><b>Build:</b> #${env.BUILD_NUMBER}</li>
                      <li><b>Commit:</b> ${env.GIT_SHORT}</li>
                      <li><b>Image:</b> ${IMAGE_NAME}:${IMAGE_TAG}</li>
                      <li><b>Console:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></li>
                    </ul>
                    <p>The stack is deployed and healthy on port ${HOST_PORT}.</p>
                """
            )
        }

        failure {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "FAILURE - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                attachLog: true,
                body: """
                    <h3>Build failed</h3>
                    <ul>
                      <li><b>Job:</b> ${env.JOB_NAME}</li>
                      <li><b>Build:</b> #${env.BUILD_NUMBER}</li>
                      <li><b>Failed stage:</b> ${env.STAGE_NAME}</li>
                      <li><b>Console:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></li>
                    </ul>
                    <p>The console log is attached.</p>
                """
            )
        }

        unstable {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "UNSTABLE - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Some tests failed. See ${env.BUILD_URL}testReport for the report."
            )
        }
    }
}
