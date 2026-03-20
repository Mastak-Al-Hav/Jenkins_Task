pipeline {
    agent any

    parameters {
        choice(name: 'TOOL', choices: ['JMeter', 'Gatling', 'Lighthouse'], description: 'Select the performance testing tool to run')
        string(name: 'TARGET_HOST', defaultValue: 'wp', description: 'Target host or IP (without http://)')
        string(name: 'TARGET_PORT', defaultValue: '80', description: 'Target port')
        string(name: 'USERS_OR_RATE', defaultValue: '9', description: 'Number of users (Gatling) or Target Rate (JMeter)')
        string(name: 'RAMP_UP', defaultValue: '1', description: 'Ramp-up duration (in minutes)')
        string(name: 'HOLD_FOR', defaultValue: '9', description: 'Steady state hold duration (in minutes)')
    }

    environment {
        WORKSPACE_DIR = "${env.WORKSPACE}"
    }

    stages {
        stage('Prepare Workspace') {
            steps {
                script {
                    echo "Cleaning up old reports and target directories..."
                    if (isUnix()) {
                        sh "rm -rf reports testResults gatling/target"
                        sh "mkdir -p reports testResults"
                    } else {
                        bat "if exist reports rmdir /s /q reports"
                        bat "mkdir reports"
                        bat "if exist testResults rmdir /s /q testResults"
                        bat "mkdir testResults"
                    }
                }
            }
        }

        stage('Run Selected Test') {
            steps {
                script {
                    if (params.TOOL == 'Gatling') {
                        echo "Running Gatling performance test..."
                        dir("gatling") { 
                            def mvnCmd = "mvn clean gatling:test -DbaseUrl=http://${params.TARGET_HOST}:${params.TARGET_PORT}/ -Dusers=${params.USERS_OR_RATE} -Dramp=${params.RAMP_UP} -Dhold=${params.HOLD_FOR}"
                            if (isUnix()) { sh mvnCmd } else { bat mvnCmd }
                        }
                    } 
                    else if (params.TOOL == 'JMeter') {
                        echo "Running JMeter performance test..."
                        dir("jmeter") {
                            def jmeterCmd = "jmeter -n -t Educart.jmx -Jhost=${params.TARGET_HOST} -Jport=${params.TARGET_PORT} -JtargetRate=${params.USERS_OR_RATE} -JrampUp=${params.RAMP_UP} -JholdFor=${params.HOLD_FOR} -l ../reports/JMeter.jtl -e -o ../reports/HtmlReport"
                            if (isUnix()) { sh jmeterCmd } else { bat jmeterCmd }
                        }
                    } 
                    else if (params.TOOL == 'Lighthouse') {
                        echo "Running Lighthouse test via Docker..."
                        dir("lighthouse") {
                            def targetUrl = "http://${params.TARGET_HOST}:${params.TARGET_PORT}"
                            def dockerCmd = "docker run --rm --network pte-network --add-host=localhost:host-gateway -v \"${WORKSPACE_DIR}/lighthouse:/usr/src/app\" -w \"/usr/src/app\" ibombit/lighthouse-puppeteer-chrome:latest node script.js ${targetUrl}"
                            
                            if (isUnix()) { sh dockerCmd } else { bat dockerCmd }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Publishing artifacts and HTML reports..."
            script {
                if (params.TOOL == 'Gatling') {
                    gatlingArchive()
                    publishHTML(target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'gatling/target/gatling',
                        reportFiles: '**/index.html',
                        reportName: 'Gatling HTML Report'
                    ])
                    archiveArtifacts artifacts: 'gatling/target/gatling/**/*', allowEmptyArchive: true
                } 
                else if (params.TOOL == 'JMeter') {
                    archiveArtifacts artifacts: 'reports/JMeter.jtl, reports/HtmlReport/**/*', allowEmptyArchive: true
                } 
                else if (params.TOOL == 'Lighthouse') {
                    archiveArtifacts artifacts: 'testResults/*.html', allowEmptyArchive: true
                }
            }
        }
    }
}
