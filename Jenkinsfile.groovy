pipeline {
    agent any

    // Requires a configured NodeJS installation via https://plugins.jenkins.io/nodejs/
    tools { nodejs "NodeJS 18.4.0" }

    stages {
        stage('git clone') {
            steps {
                git url: 'https://github.com/mattatsnyk/TodoList-.net'
            }
        }

        // Install the Snyk CLI with npm. For more information, check:
        // https://docs.snyk.io/snyk-cli/install-the-snyk-cli
        stage('Install snyk CLI') {
            steps {
                script {
                    sh 'npm install -g snyk'
                    sh 'npm install -g snyk-to-html'
                }
            }
        }

        // This OPTIONAL step will configure the Snyk CLI to connect to the EU instance of Snyk.
        // stage('Configure Snyk for EU data center') {
        //     steps {
        //         sh 'snyk config set use-base64-encoding=true'
        //         sh 'snyk config set endpoint='https://app.eu.snyk.io/api'
        //     }
        // }

        // Authorize the Snyk CLI
        stage('Authorize Snyk CLI') {
            steps {
                withCredentials([string(credentialsId: 'SNYK_TOKEN', variable: 'SNYK_TOKEN')]) {
                    sh 'snyk auth ${SNYK_TOKEN}'
                }
            }
        }

        stage('Build App') {
            steps {
                // Replace this with your build instructions, as necessary.
                sh '/usr/local/share/dotnet/dotnet restore'
            }
        }

        stage('Snyk') {
            parallel {
                stage('Snyk Open Source') {
                    steps {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            sh 'snyk test --all-projects --sarif-file-output=results-open-source.sarif'
                        }
                        recordIssues tool: sarif(name: 'Snyk Open Source', id: 'snyk-open-source', pattern: 'results-open-source.sarif')
                    }
                }
            }
        }
    }
} 
