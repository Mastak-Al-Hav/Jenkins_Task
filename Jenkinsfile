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

        stage('Auto-Install JMeter') {
            
            when { expression { params.TOOL == 'JMeter' } }
            steps {
                script {
                    echo "Перевірка наявності JMeter..."
                    if (isUnix()) {
                        sh """
                        if [ ! -d "${JMETER_DIR}" ]; then
                            echo "JMeter не знайдено. Завантажую..."
                            wget -q https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VER}.tgz
                            tar -xzf apache-jmeter-${JMETER_VER}.tgz
                            rm apache-jmeter-${JMETER_VER}.tgz
                        else
                            echo "JMeter вже встановлено у workspace."
                        fi
                        """
                    } else {
                        bat """
                        if not exist "${JMETER_DIR}" (
                            echo JMeter не знайдено. Завантажую через PowerShell...
                            powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-%JMETER_VER%.zip' -OutFile 'jmeter.zip'"
                            powershell -Command "Expand-Archive -Path 'jmeter.zip' -DestinationPath '%WORKSPACE_DIR%' -Force"
                            del jmeter.zip
                        ) else (
                            echo JMeter вже встановлено у workspace.
                        )
                        """
                    }
                }
            }
        }

        stage('Run Selected Test') {
            steps {
                script {
                    if (params.TOOL == 'Gatling') {
                        echo "Запуск Gatling через Maven Wrapper..."
                        dir("gatling") { 
                            
                            if (isUnix()) {
                                sh "chmod +x mvnw" 
                                sh "./mvnw clean gatling:test -DbaseUrl=http://${params.TARGET_HOST}:${params.TARGET_PORT}/ -Dusers=${params.USERS_OR_RATE} -Dramp=${params.RAMP_UP} -Dhold=${params.HOLD_FOR}"
                            } else {
                                bat "mvnw.cmd clean gatling:test -DbaseUrl=http://${params.TARGET_HOST}:${params.TARGET_PORT}/ -Dusers=${params.USERS_OR_RATE} -Dramp=${params.RAMP_UP} -Dhold=${params.HOLD_FOR}"
                            }
                        }
                    } 
                    else if (params.TOOL == 'JMeter') {
                        echo "Запуск JMeter..."
                        dir("jmeter") {
                            def jmeterExec = isUnix() ? "${JMETER_DIR}/bin/jmeter" : "${JMETER_DIR}\\bin\\jmeter.bat"
                            def jmeterCmd = "\"${jmeterExec}\" -n -t Educart.jmx -Jhost=${params.TARGET_HOST} -Jport=${params.TARGET_PORT} -JtargetRate=${params.USERS_OR_RATE} -JrampUp=${params.RAMP_UP} -JholdFor=${params.HOLD_FOR} -l ../reports/JMeter.jtl -e -o ../reports/HtmlReport"
                            
                            if (isUnix()) { sh jmeterCmd } else { bat jmeterCmd }
                        }
                    } 
                    else if (params.TOOL == 'Lighthouse') {
                        echo "Запуск Lighthouse через чистий Node.js..."
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
