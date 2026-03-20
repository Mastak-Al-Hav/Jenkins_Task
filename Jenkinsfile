pipeline {
    agent any

    parameters {
        choice(name: 'TOOL', choices: ['JMeter', 'Gatling', 'Lighthouse'], description: 'Select the performance testing tool to execute')
        string(name: 'TARGET_HOST', defaultValue: 'wp', description: 'Target host URL or IP')
        string(name: 'TARGET_PORT', defaultValue: '80', description: 'Target port')
        string(name: 'USERS_OR_RATE', defaultValue: '9', description: 'Target Users (Gatling) or Arrival Rate (JMeter)')
        string(name: 'RAMP_UP', defaultValue: '1', description: 'Ramp-up duration (minutes)')
        string(name: 'HOLD_FOR', defaultValue: '9', description: 'Steady state duration (minutes)')
    }

    environment {
        WORKSPACE_DIR = "${env.WORKSPACE}"
        JMETER_VER    = "5.6.3"
        JMETER_DIR    = "${env.WORKSPACE}/apache-jmeter-${JMETER_VER}"
        CASUTG_PLUGIN_URL = "https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-casutg/2.10/jmeter-plugins-casutg-2.10.jar"
    }

    stages {
        stage('Prepare Workspace') {
            steps {
                script {
                    echo "Cleaning up workspace..."
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

        stage('Check & Prepare Tools') {
            steps {
                script {
                    if (params.TOOL == 'JMeter') {
                        echo "Checking for JMeter..."
                        def jmeterStatus = isUnix() ? sh(script: 'jmeter -v', returnStatus: true) : bat(script: 'jmeter -v >nul 2>&1', returnStatus: true)
                        
                        if (jmeterStatus == 0) {
                            echo "System JMeter detected."
                            env.JMETER_EXEC = "jmeter"
                        } else {
                            echo "Setting up local JMeter environment..."
                            if (isUnix()) {
                                sh """
                                if [ ! -d "${JMETER_DIR}" ]; then
                                    echo "Downloading JMeter binaries..."
                                    wget -q https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VER}.tgz
                                    tar -xzf apache-jmeter-${JMETER_VER}.tgz
                                    rm apache-jmeter-${JMETER_VER}.tgz
                                fi
                                if [ ! -f "${JMETER_DIR}/lib/ext/jmeter-plugins-casutg-2.10.jar" ]; then
                                    echo "Downloading required JMeter plugin..."
                                    wget -q ${CASUTG_PLUGIN_URL} -P ${JMETER_DIR}/lib/ext/
                                fi
                                """
                                env.JMETER_EXEC = "${JMETER_DIR}/bin/jmeter"
                            } else {
                                bat """
                                if not exist "${JMETER_DIR}" (
                                    echo Downloading JMeter binaries...
                                    powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-%JMETER_VER%.zip' -OutFile 'jmeter.zip'"
                                    powershell -Command "Expand-Archive -Path 'jmeter.zip' -DestinationPath '%WORKSPACE_DIR%' -Force"
                                    del jmeter.zip
                                )
                                if not exist "${JMETER_DIR}\\lib\\ext\\jmeter-plugins-casutg-2.10.jar" (
                                    echo Plugin missing. Downloading required library...
                                    powershell -Command "Invoke-WebRequest -Uri '${CASUTG_PLUGIN_URL}' -OutFile '${JMETER_DIR}\\lib\\ext\\jmeter-plugins-casutg-2.10.jar'"
                                )
                                """
                                env.JMETER_EXEC = "${JMETER_DIR}\\bin\\jmeter.bat"
                            }
                        }
                    } 
                    else if (params.TOOL == 'Gatling') {
                        echo "Preparing Gatling tool..."
                        def mvnStatus = isUnix() ? sh(script: 'mvn -v', returnStatus: true) : bat(script: 'mvn -v >nul 2>&1', returnStatus: true)
                        if (mvnStatus == 0) {
                            env.MVN_EXEC = "mvn"
                        } else {
                            if (isUnix()) { sh "chmod +x gatling/mvnw" }
                            env.MVN_EXEC = isUnix() ? "./mvnw" : "mvnw.cmd"
                        }
                    }
                    else if (params.TOOL == 'Lighthouse') {
                        echo "Checking Node.js environment..."
                        def nodeStatus = isUnix() ? sh(script: 'node -v', returnStatus: true) : bat(script: 'node -v >nul 2>&1', returnStatus: true)
                        if (nodeStatus != 0) { error("Node.js not found on this agent.") }
                    }
                }
            }
        }

        stage('Run Performance Test') {
            steps {
                script {
                    def targetUrl = "http://${params.TARGET_HOST}:${params.TARGET_PORT}"
                    
                    if (params.TOOL == 'Gatling') {
                        dir("gatling") { 
                            def mvnCmd = "${env.MVN_EXEC} clean gatling:test -DbaseUrl=${targetUrl}/ -Dusers=${params.USERS_OR_RATE} -Dramp=${params.RAMP_UP} -Dhold=${params.HOLD_FOR}"
                            if (isUnix()) { sh mvnCmd } else { bat mvnCmd }
                        }
                    } 
                    else if (params.TOOL == 'JMeter') {
                        dir("jmeter") {
                            def jmeterCmd = "\"${env.JMETER_EXEC}\" -n -t Educart.jmx -Jhost=${params.TARGET_HOST} -Jport=${params.TARGET_PORT} -JtargetRate=${params.USERS_OR_RATE} -JrampUp=${params.RAMP_UP} -JholdFor=${params.HOLD_FOR} -l ../reports/JMeter.jtl -e -o ../reports/HtmlReport"
                            if (isUnix()) { sh jmeterCmd } else { bat jmeterCmd }
                        }
                    } 
                    else if (params.TOOL == 'Lighthouse') {
                        dir("lighthouse") {
                            if (isUnix()) { 
                                sh "npm install puppeteer lighthouse"
                                sh "node script.js ${targetUrl}" 
                            } else { 
                                bat "npm install puppeteer lighthouse"
                                bat "node script.js ${targetUrl}" 
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Archiving results..."
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
