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
                        npmVersion()
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
            stage('Create Zip Artifact') {
                steps {
                    echo "STAGE 4 - Create Artifact"
                    zip archive: true, glob: 'build/*.*', zipFile: "${config.artifact}", overwrite: true
                }
            }
            stage('reused'){
                steps{
                                reactAwsS3Pipeline()

                }
            }
            stage('Upload Artifact to S3 Bucket') {
                steps {
                    input "Upload Artifact ${config.artifact} to S3 Bucket ${config.s3Artifact}?"
                    println artifact
                    println s3Artifact
                    withAWS(credentials: "${config.awsCredentials}", region: "${config.awsRegion}") {
                        s3Upload(file:"${config.artifact}", bucket:"${config.s3Artifact}", path:"${config.artifact}")
                    }
                }
            }
            stage('Deploy Artifact to S3 Bucket') {
                steps {
                    input "Deploy Artifact ${config.artifact} to S3 Bucket ${config.s3Artifact}?"
                    println artifact
                    println s3Artifact
                    withAWS(credentials: "${config.awsCredentials}", region: "${config.awsRegion}") {
                        dir("build") {
                            script {
                                files = findFiles(glob: '**')
                                files.each { 
                                    println "File:  ${it}"
                                    s3Upload(file:"${it}", bucket:"${config.s3Artifact}", path:"${it}")
                                }
                            }
                        }
                    }
                }
            }
            stage('Check Application is Up and Running') {
                steps {
                    echo "Check webpage is deployed"
                    timeout(300) {
                        waitUntil {
                            script {
                                def r = sh script: "curl -s http://${config.s3Artifact}.s3-website-${config.awsRegion}.amazonaws.com/", returnStatus: true
                                return (r == 0);
                            }
                        }
                    }
                }
            } 
        }
    }
}
