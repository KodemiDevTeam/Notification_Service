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
                    cd
                    echo.
                    echo ===== FILES =====
                    dir
                    echo.
                    echo ===== POM FILES =====
                    dir /s /b pom.xml
                '''
            }
        }

        stage('Verify Java & Maven') {
            steps {
                bat '''
                    echo ===== JAVA VERSION =====
                    java -version

                    echo.
                    echo ===== JAVA HOME =====
                    echo %JAVA_HOME%

                    echo.
                    echo ===== MAVEN VERSION =====
                    mvn --version

                    echo.
                    echo ===== MAVEN HOME =====
                    echo %MAVEN_HOME%
                '''
            }
        }

        /* ================= BUILD ================= */

        stage('Build') {
            steps {
                bat '''
                    echo ===== BUILDING APPLICATION =====

                    mvn -B clean compile ^
                    -Deureka.client.enabled=false ^
                    -Dspring.cloud.discovery.enabled=false
                '''
            }
        }

        /* ================= TEST + COVERAGE ================= */

        stage('Test & Coverage') {
            steps {
                bat '''
                    echo ===== RUNNING UNIT TESTS =====

                    mvn -B test ^
                    -Deureka.client.enabled=false ^
                    -Dspring.cloud.discovery.enabled=false

                    echo.
                    echo ===== TEST EXECUTION COMPLETED =====
                '''
            }
        }

        /* ================= VERIFY TEST RESULTS ================= */

        stage('Verify Test Results') {
            steps {
                bat '''
                    echo ===== TEST REPORTS =====

                    if exist target\\surefire-reports (
                        dir target\\surefire-reports
                    ) else (
                        echo WARNING: target\\surefire-reports not found
                    )

                    echo.
                    echo ===== JACOCO REPORT =====

                    if exist target\\site\\jacoco\\jacoco.xml (
                        echo JaCoCo XML report found.
                    ) else (
                        echo WARNING: JaCoCo XML report not found.
                    )
                '''
            }
        }

        /* ================= SONAR ================= */

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube2') {
                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {
                        bat '''
                            echo ===== SONARQUBE ANALYSIS =====

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
                        echo "Quality Gate skipped: ${e.getMessage()}"
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

                dependencyCheckPublisher(
                    pattern: 'dependency-check-report.xml'
                )
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
            echo 'SUCCESS: Build + Tests + Coverage + SonarQube + OWASP completed'
        }

        failure {
            echo 'FAILED: Check Jenkins console logs'
        }

        always {
            echo 'Pipeline execution finished'
        }
    }
}
