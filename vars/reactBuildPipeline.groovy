/* groovylint-disable MethodParameterTypeRequired, MethodReturnTypeRequired, NoDef, VariableTypeRequired */
def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent any
        stages {
            stage('Checkout Source') {
                steps {
                    // git credentialsId: 'dc69dd47-d601-4cb0-adbe-548c17e15506', url: "http://<gitRepo>/<username>/<repoName>.git"
                    checkout scm
                    script {
                        def packageJSON = readJSON file: 'package.json'
                        def packageJSONVersion = packageJSON.version
                        devTag  = "${packageJSONVersion}-" + currentBuild.number
                        prodTag = "${packageJSONVersion}"
                        echo packageJSONVersion
                    }
                }
            }
            stage('Install dependencies') {
                steps {
                    sh "${config.npmCmd} install"
                }
            }
            stage('Build React App Dev') {
                steps {
                    echo "Building version ${config.devTag}"
                    sh "${config.npmCmd} run build"
                }
            }
            stage('Unit Tests') {
                steps {
                    echo 'Running Unit Tests'
                    sh "${config.npmCmd} test -- --watchAll=false"
                }
            }
            stage('Code Analysis') {
                steps {
                    script {
                        echo 'Running Code Analysis'
                    }
                }
            }
            stage('Publish to Nexus') {
                steps {
                    echo 'Publish to Nexus'
                }
            }
        }
    }
}
