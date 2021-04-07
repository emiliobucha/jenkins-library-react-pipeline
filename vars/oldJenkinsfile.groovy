// Set variable globally to be available in all stages

//def mvnCmd      = "mvn -s ./nexus_settings.xml"
def npmCmd      = "npm"

// Set the tag for the development image: version + build number
def devTag      = "0.0-0"

// Set the tag for the production image: version
def prodTag     = "0.0"

// Artifact
def prefix = "semperti"
def app = "react"
def env = "development-rapientrega"
def artifact = "${app}.zip"
def s3Artifact = "${prefix}-${app}-${env}"

// AWS
//def awsCredentials = "aws-credentials-jenkins-s3"
def awsCredentials = "aws-bucha"
def awsRegion = "us-east-1"
def awsProfile = "semperti"

pipeline {

    agent any
    //environment {
    //    ANSIBLE_HOST_KEY_CHECKING = 'false'
    //}
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
                sh "${npmCmd} install"
            }
        }
        stage('Build React App Dev') {
            steps {
                echo "Building version ${devTag}"
                sh "${npmCmd} run build"
            }
        }
        stage('Unit Tests') {
            steps {
                echo "Running Unit Tests"
                sh "${npmCmd} test -- --watchAll=false"
            }
        }
        stage('Code Analysis') {
            steps {
                script {
                echo "Running Code Analysis"
                }
            }
        }
        stage('Publish to Nexus') {
            steps {
                echo "Publish to Nexus"
            }
        }
        stage('Create Zip Artifact') {
            steps {
                echo "STAGE 4 - Create Artifact"
                zip archive: true, glob: 'build/*.*', zipFile: "${artifact}", overwrite: true
            }
        }
        stage('Upload Artifact to S3 Bucket') {
            steps {
                input "Upload Artifact ${artifact} to S3 Bucket ${s3Artifact}?"
                println artifact
                println s3Artifact
                withAWS(credentials: "${awsCredentials}", region: "${awsRegion}") {
                    s3Upload(file:"${artifact}", bucket:"${s3Artifact}", path:"${artifact}")
                }
            }
        }
        stage('Deploy Artifact to S3 Bucket') {
            steps {
                input "Deploy Artifact ${artifact} to S3 Bucket ${s3Artifact}?"
                println artifact
                println s3Artifact
                withAWS(credentials: "${awsCredentials}", region: "${awsRegion}") {                    
                    dir("build") {
                        script {
                            files = findFiles(glob: '**')
                            files.each { 
                                println "File:  ${it}"
                                s3Upload(file:"${it}", bucket:"${s3Artifact}", path:"${it}")
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
                            def r = sh script: "curl -s http://${s3Artifact}.s3-website-${awsRegion}.amazonaws.com/", returnStatus: true
                            return (r == 0);
                        }
                    }
                }
            }
        }
    }
}
