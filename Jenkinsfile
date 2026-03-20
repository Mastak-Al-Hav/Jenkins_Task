pipeline {
    agent any

    parameters {
        choice(name: 'TOOL', choices: ['JMeter', 'Gatling', 'Lighthouse'], description: 'Оберіть інструмент для тестування')
        string(name: 'TARGET_HOST', defaultValue: 'wp', description: 'Цільовий хост (без http://)')
        string(name: 'TARGET_PORT', defaultValue: '80', description: 'Цільовий порт')
        string(name: 'USERS_OR_RATE', defaultValue: '9', description: 'Кількість юзерів / Target Rate')
        string(name: 'RAMP_UP', defaultValue: '1', description: 'Час розгону (Ramp-up у хвилинах)')
        string(name: 'HOLD_FOR', defaultValue: '9', description: 'Тривалість тесту (Hold у хвилинах)')
    }

    environment {
        WORKSPACE_DIR = "${env.WORKSPACE}"
        JMETER_VER = "5.6.3"
        JMETER_DIR = "${env.WORKSPACE}/apache-jmeter-${JMETER_VER}"
    }

    stages {
        stage('Prepare Workspace') {
            steps {
                script {
                    echo "Очищення старих звітів..."
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
                        echo "Перевірка наявності JMeter..."
                        
                        def jmeterStatus = isUnix() ? sh(script: 'jmeter -v', returnStatus: true) : bat(script: 'jmeter -v >nul 2>&1', returnStatus: true)
                        
                        if (jmeterStatus == 0) {
                            echo "Знайдено системний JMeter. Використовуємо його."
                            env.JMETER_EXEC = "jmeter"
                        } else {
                            echo "Системний JMeter не знайдено. Перевірка локальної копії..."
                            if (isUnix()) {
                                sh """
                                if [ ! -d "${JMETER_DIR}" ]; then
                                    echo "Завантажую JMeter..."
                                    wget -q https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VER}.tgz
                                    tar -xzf apache-jmeter-${JMETER_VER}.tgz
                                    rm apache-jmeter-${JMETER_VER}.tgz
                                fi
                                """
                                env.JMETER_EXEC = "${JMETER_DIR}/bin/jmeter"
                            } else {
                                bat """
                                if not exist "${JMETER_DIR}" (
                                    echo Завантажую JMeter через PowerShell...
                                    powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-%JMETER_VER%.zip' -OutFile 'jmeter.zip'"
                                    powershell -Command "Expand-Archive -Path 'jmeter.zip' -DestinationPath '%WORKSPACE_DIR%' -Force"
                                    del jmeter.zip
                                )
                                """
                                env.JMETER_EXEC = "${JMETER_DIR}\\bin\\jmeter.bat"
                            }
                        }
                    } 
                    else if (params.TOOL == 'Gatling') {
                        echo "Перевірка наявності Maven..."
                        def mvnStatus = isUnix() ? sh(script: 'mvn -v', returnStatus: true) : bat(script: 'mvn -v >nul 2>&1', returnStatus: true)
                        
                        if (mvnStatus == 0) {
                            echo "Знайдено системний Maven. Використовуємо його."
                            env.MVN_EXEC = "mvn"
                        } else {
                            echo "Системний Maven не знайдено. Буде використано Maven Wrapper (mvnw)."
                            if (isUnix()) {
                                sh "chmod +x gatling/mvnw" 
                            }
                            env.MVN_EXEC = isUnix() ? "./mvnw" : "mvnw.cmd"
                        }
                    }
                    else if (params.TOOL == 'Lighthouse') {
                        echo "Перевірка наявності Node.js та NPM..."
                        def nodeStatus = isUnix() ? sh(script: 'node -v', returnStatus: true) : bat(script: 'node -v >nul 2>&1', returnStatus: true)
                        def npmStatus = isUnix() ? sh(script: 'npm -v', returnStatus: true) : bat(script: 'npm -v >nul 2>&1', returnStatus: true)
                        
                        if (nodeStatus != 0 || npmStatus != 0) {
                            
                            error("Node.js або NPM не встановлено на цьому сервері Jenkins! Будь ласка, встановіть їх глобально.")
                        } else {
                            echo "Node.js та NPM знайдено."
                        }
                    }
                }
            }
        }

        stage('Run Selected Test') {
            steps {
                script {
                    if (params.TOOL == 'Gatling') {
                        echo "Запуск тесту Gatling..."
                        dir("gatling") { 

                            def mvnCmd = "${env.MVN_EXEC} clean gatling:test -DbaseUrl=http://${params.TARGET_HOST}:${params.TARGET_PORT}/ -Dusers=${params.USERS_OR_RATE} -Dramp=${params.RAMP_UP} -Dhold=${params.HOLD_FOR}"
                            if (isUnix()) { sh mvnCmd } else { bat mvnCmd }
                        }
                    } 
                    else if (params.TOOL == 'JMeter') {
                        echo "Запуск тесту JMeter..."
                        dir("jmeter") {
                
                            def jmeterCmd = "\"${env.JMETER_EXEC}\" -n -t Educart.jmx -Jhost=${params.TARGET_HOST} -Jport=${params.TARGET_PORT} -JtargetRate=${params.USERS_OR_RATE} -JrampUp=${params.RAMP_UP} -JholdFor=${params.HOLD_FOR} -l ../reports/JMeter.jtl -e -o ../reports/HtmlReport"
                            
                            if (isUnix()) { sh jmeterCmd } else { bat jmeterCmd }
                        }
                    } 
                    else if (params.TOOL == 'Lighthouse') {
                        echo "Встановлення залежностей та запуск Lighthouse..."
                        dir("lighthouse") {
                            def targetUrl = "http://${params.TARGET_HOST}:${params.TARGET_PORT}"
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
            echo "Збереження звітів..."
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
