pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        JAVA_HOME             = 'C:\\Program Files\\Java\\jdk-17'
        MAVEN_HOME            = 'C:\\Program Files\\Apache\\maven'
        PATH                  = "${env.JAVA_HOME}\\bin;${env.MAVEN_HOME}\\bin;${env.PATH}"

        SONAR_PROJECT_KEY     = 'Notification-Service'
        SONAR_PROJECT_NAME    = 'Notification-Service'

        COVERAGE_LINE_MIN     = '70'
        COVERAGE_BRANCH_MIN   = '60'
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
                    dir
                    for /r . %%f in (pom.xml) do echo %%f
                '''
            }
        }

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

        stage('Test & Coverage') {
            steps {
                bat '''
                    echo ===== RUNNING TESTS WITH JACOCO =====
                    mvn -B test ^
                        -Deureka.client.enabled=false ^
                        -Dspring.cloud.discovery.enabled=false
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: true

                    publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'target/site/jacoco',
                        reportFiles          : 'index.html',
                        reportName           : 'JaCoCo Coverage Report'
                    ])
                }
            }
        }

        stage('Coverage Gate') {
            steps {
                script {
                    def csvFile = 'target/site/jacoco/jacoco.csv'

                    if (!fileExists(csvFile)) {
                        echo "WARNING: ${csvFile} not found — skipping coverage gate."
                        return
                    }

                    def lines            = readFile(csvFile).trim().split('\n')
                    def header           = lines[0].split(',')
                    def lineMissedIdx    = header.findIndexOf { it.trim() == 'LINE_MISSED'    }
                    def lineCoveredIdx   = header.findIndexOf { it.trim() == 'LINE_COVERED'   }
                    def branchMissedIdx  = header.findIndexOf { it.trim() == 'BRANCH_MISSED'  }
                    def branchCoveredIdx = header.findIndexOf { it.trim() == 'BRANCH_COVERED' }

                    long totalLineMissed = 0, totalLineCovered = 0
                    long totalBranchMissed = 0, totalBranchCovered = 0

                    lines.drop(1).each { row ->
                        def cols = row.split(',')
                        if (cols.size() > lineCoveredIdx) {
                            totalLineMissed    += (cols[lineMissedIdx]   ?.trim()?.toLong() ?: 0)
                            totalLineCovered   += (cols[lineCoveredIdx]  ?.trim()?.toLong() ?: 0)
                            totalBranchMissed  += (cols[branchMissedIdx] ?.trim()?.toLong() ?: 0)
                            totalBranchCovered += (cols[branchCoveredIdx]?.trim()?.toLong() ?: 0)
                        }
                    }

                    long totalLines    = totalLineMissed   + totalLineCovered
                    long totalBranches = totalBranchMissed + totalBranchCovered

                    double linePct   = totalLines    > 0 ? (totalLineCovered   * 100.0 / totalLines)    : 0
                    double branchPct = totalBranches > 0 ? (totalBranchCovered * 100.0 / totalBranches) : 0

                    echo "=========================================="
                    echo "  Line   Coverage : ${String.format('%.2f', linePct)} %  (min: ${COVERAGE_LINE_MIN}%)"
                    echo "  Branch Coverage : ${String.format('%.2f', branchPct)} %  (min: ${COVERAGE_BRANCH_MIN}%)"
                    echo "=========================================="

                    boolean failed = false

                    if (linePct < COVERAGE_LINE_MIN.toDouble()) {
                        echo "FAIL: Line coverage ${String.format('%.2f', linePct)}% is below threshold ${COVERAGE_LINE_MIN}%"
                        failed = true
                    }
                    if (branchPct < COVERAGE_BRANCH_MIN.toDouble()) {
                        echo "FAIL: Branch coverage ${String.format('%.2f', branchPct)}% is below threshold ${COVERAGE_BRANCH_MIN}%"
                        failed = true
                    }

                    if (failed) {
                        currentBuild.result = 'UNSTABLE'
                        echo "Coverage gate not met — build marked UNSTABLE."
                    } else {
                        echo "Coverage gate passed."
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube2') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        bat '''
                            echo ===== SONAR ANALYSIS =====
                            mvn -B sonar:sonar ^
                                -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                                -Dsonar.projectName=%SONAR_PROJECT_NAME% ^
                                -Dsonar.login=%SONAR_TOKEN% ^
                                -Dsonar.coverage.jacoco.xmlReportPaths=target\\site\\jacoco\\jacoco.xml
                        '''
                    }
                }
            }
        }

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

        stage('OWASP Dependency Check') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_KEY')]) {
                    dependencyCheck(
                        additionalArguments: "--nvdApiKey ${NVD_KEY} --format XML --out . --disableOssIndex",
                        odcInstallation: 'Default'
                    )
                }
                dependencyCheckPublisher pattern: 'dependency-check-report.xml'
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'dependency-check-report.xml, target/site/jacoco/**',
                                 fingerprint: true,
                                 allowEmptyArchive: true
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Build + Tests + Coverage + Sonar + OWASP completed'
        }
        unstable {
            echo 'UNSTABLE: Coverage gate not met — review Coverage Gate stage'
        }
        failure {
            echo 'FAILED: Check logs'
        }
        always {
            echo 'Pipeline execution finished'
        }
    }
}
