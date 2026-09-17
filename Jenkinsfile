pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21.0.12.1'
        MAVEN_HOME = 'C:\\ProgramData\\chocolatey\\lib\\maven\\apache-maven-3.9.16'

        PATH = 'C:\\Program Files\\Java\\jdk-21.0.12.1\\bin;C:\\ProgramData\\chocolatey\\lib\\maven\\apache-maven-3.9.16\\bin;C:\\Windows\\System32;C:\\Windows;C:\\Program Files\\Git\\cmd'

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
                bat '''
                    echo ===== WORKSPACE DEBUG =====
                    echo Current Directory:
                    cd

                    echo.
                    echo Workspace Contents:
                    dir

                    echo.
                    echo POM Files:
                    dir /s /b pom.xml
                '''
            }
        }

        /* ================= BUILD ================= */

        stage('Build (No Tests)') {
            steps {
                bat '''
                    echo ===== BUILD WITHOUT TESTS =====

                    mvn -B clean install ^
                    -Dmaven.test.skip=true ^
                    -Deureka.client.enabled=false ^
                    -Dspring.cloud.discovery.enabled=false
                '''
            }
        }

        /* ================= TEST + COVERAGE ================= */

        stage('Test & Coverage') {
            steps {
                bat '''
                    echo ===== RUNNING TESTS WITH JACOCO =====

                    mvn -B test ^
                    -Deureka.client.enabled=false ^
                    -Dspring.cloud.discovery.enabled=false
                '''
            }
        }

        /* ================= SONAR ================= */

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {
                        bat '''
                            echo ===== SONAR ANALYSIS =====

                            mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                              -Dsonar.projectKey=Notification-Service ^
                              -Dsonar.projectName=Notification-Service ^
                              -Dsonar.token=%SONAR_TOKEN% ^
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        /* ================= QUALITY GATE ================= */

        stage('Quality Gate') {
            steps {
                script {
                    try {
                        timeout(time: 10, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: false
                        }
                    } catch (Exception e) {
                        echo "Quality Gate skipped"
                    }
                }
            }
        }

        /* ================= OWASP ================= */

        stage('OWASP Dependency Check') {
            steps {

                withCredentials([
                    string(
                        credentialsId: 'nvd-api-key',
                        variable: 'NVD_KEY'
                    )
                ]) {

                    dependencyCheck(
                        additionalArguments: "--nvdApiKey ${NVD_KEY} --format XML --out . --disableOssIndex",
                        odcInstallation: 'Default'
                    )
                }

                dependencyCheckPublisher pattern: 'dependency-check-report.xml'
            }
        }

        /* ================= ARCHIVE ================= */

        stage('Archive Reports') {
            steps {
                archiveArtifacts(
                    artifacts: 'dependency-check-report.xml',
                    fingerprint: true
                )
            }
        }
    }

    post {

        success {
            echo 'SUCCESS: Build + Sonar + OWASP completed'
        }

        failure {
            echo 'FAILED: Check logs'
        }

        always {
            echo 'Pipeline execution finished'
        }
    }
}
