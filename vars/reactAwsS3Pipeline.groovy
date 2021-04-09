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
            stage('Upload Artifact to S3 Bucket') {
                steps {
                    script{
                        awsS3Upload.upload(config.artifact, config.s3Artifact, config.awsCredentials, config.awsRegion)
                    }
                }
            }
            stage('Deploy Artifact to S3 Bucket') {
                steps {
                    script{
                        awsS3Upload.uploadFolder(config.buildFolder, config.s3Artifact, config.awsCredentials, config.awsRegion)
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
