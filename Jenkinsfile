
pipeline {
    agent any

    tools {
        jdk 'JDK 17'
        maven 'Maven'
    }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
        timestamps()
    }

    environment {
        SONAR_PROJECT_KEY  = 'Notification-Service'
        SONAR_PROJECT_NAME = 'Notification-Service'
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Debug Workspace') {
            steps {
                sh '''
                    set -eu

                    echo "===== WORKSPACE ====="
                    pwd
                    ls -la

                    echo "===== POM FILES ====="
                    find . -name pom.xml -not -path '*/.git/*' -print

                    echo "===== JAVA ====="
                    java -version

                    echo "===== MAVEN ====="
                    mvn -version
                '''
            }
        }

        stage('Build Without Tests') {
            steps {
                sh '''
                    set -eu

                    echo "===== BUILDING WITHOUT TESTS ====="

                    mvn -B \
                        -Dmaven.test.skip=true \
                        clean package \
                        -Deureka.client.enabled=false \
                        -Dspring.cloud.discovery.enabled=false

                    echo "BUILD COMPLETED SUCCESSFULLY"
                '''
            }
        }

        stage('Validate Build Output') {
            steps {
                sh '''
                    set -eu

                    if [ ! -d target/classes ]; then
                        echo "ERROR: target/classes was not generated."
                        exit 1
                    fi

                    if ! find target -maxdepth 1 -type f -name '*.jar' -print -quit | grep -q .; then
                        echo "ERROR: Application JAR was not generated."
                        exit 1
                    fi

                    echo "Application build output verified."
                    find target -maxdepth 1 -type f -name '*.jar' -print
                '''
            }
        }

        stage('OWASP Dependency Check') {
            steps {
                script {
                    catchError(
                        buildResult: 'UNSTABLE',
                        stageResult: 'UNSTABLE'
                    ) {
                        dependencyCheck(
                            odcInstallation: 'OWASP',
                            additionalArguments: '--noupdate --format HTML --format XML --format CSV --out . --disableOssIndex'
                        )
                    }
                }
            }
        }

        stage('Publish OWASP Report') {
            steps {
                script {
                    if (fileExists('dependency-check-report.xml')) {
                        dependencyCheckPublisher(
                            pattern: 'dependency-check-report.xml',
                            skipNoReportFiles: true
                        )

                        echo 'OWASP XML report submitted to Jenkins publisher.'
                    } else {
                        echo 'WARNING: OWASP XML report was not generated.'
                        currentBuild.result = 'UNSTABLE'
                    }

                    if (fileExists('dependency-check-report.html')) {
                        echo 'OWASP HTML report is available for archiving.'
                    } else {
                        echo 'WARNING: OWASP HTML report is missing.'
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Archive OWASP Reports') {
            steps {
                archiveArtifacts(
                    artifacts: 'dependency-check-report.html,dependency-check-report.xml,dependency-check-report.csv',
                    allowEmptyArchive: true,
                    fingerprint: true
                )
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def sonarJavaHome = tool(
                        name: 'JDK 21',
                        type: 'hudson.model.JDK'
                    )

                    withSonarQubeEnv('sonarscanner') {
                        withCredentials([
                            string(
                                credentialsId: 'sonartk',
                                variable: 'SONAR_TOKEN'
                            )
                        ]) {
                            withEnv([
                                "JAVA_HOME=${sonarJavaHome}",
                                "PATH+SONAR_JAVA=${sonarJavaHome}/bin"
                            ]) {
                                sh '''
                                    set -eu

                                    echo "===== JAVA FOR SONARQUBE ====="
                                    java -version

                                    echo "===== SONARQUBE ANALYSIS ====="

                                    mvn -B \
                                        org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                        -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
                                        -Dsonar.projectName="$SONAR_PROJECT_NAME" \
                                        -Dsonar.token="$SONAR_TOKEN" \
                                        -Dsonar.java.binaries=target/classes

                                    echo "SONARQUBE ANALYSIS COMPLETED"
                                '''
                            }
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate(
                            abortPipeline: false
                        )

                        echo "SonarQube Quality Gate: ${qg.status}"

                        if (qg.status != 'OK') {
                            unstable(
                                "SonarQube Quality Gate status: ${qg.status}"
                            )
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Build, OWASP scan, and SonarQube analysis completed.'
        }

        unstable {
            echo 'UNSTABLE: Review OWASP results, missing reports, or Quality Gate status.'
        }

        failure {
            echo 'FAILED: Check the first failed stage in Console Output.'
        }

        always {
            echo "Pipeline execution finished. Result: ${currentBuild.currentResult}"
        }
    }
}
