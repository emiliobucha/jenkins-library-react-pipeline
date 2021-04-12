#!/usr/bin/env groovy
def awsCredentialsId ='aws-credentials-jenkins'
pipeline {
    agent any
    
    options {
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()
        parallelsAlwaysFailFast()
        timestamps()
        
    }

    parameters {
        string(defaultValue: 'build', description: 'Static website folder', name: 'BUILD_FOLDER')
        string(defaultValue: '', description: 'Website S3 Bucket.', name: 'S3_BUCKET')
        string(defaultValue: 'us-east-1', description: 'AWS region for the pipeline.', name: 'AWS_REGION')
        credentials(defaultValue: awsCredentialsId, description: 'AWS credentials', name: 'AWS_CREDENTIALS')
        string(defaultValue: '', description: 'Builds workspace', name: 'WS_PATH')
    }
    stages{

        stage('Deploy Artifact to S3 Bucket') {
                steps {
                    script{
                        ws(params.WS_PATH){
                        // awsS3Upload.uploadFolder(params.BUILD_FOLDER, params.S3_BUCKET, params.AWS_CREDENTIALS, params.AWS_REGION)
                        //def awsCredentials = [[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: params.AWS_CREDENTIALS]]
                            input "Deploy Artifact folder ${params.BUILD_FOLDER} to S3 Bucket ${params.S3_BUCKET}?"
                            withAWS(credentials: "${params.AWS_CREDENTIALS}", region: "${params.AWS_REGION}") {
                                dir("${params.BUILD_FOLDER}") {
                                    script {
                                        files = findFiles(glob: '**')
                                        files.each { 
                                            println "File:  ${it}"
                                            s3Upload(file:"${it}", bucket:"${params.S3_BUCKET}", path:"${it}")
                                        }
                                    }
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
                            def r = sh script: "curl -s http://${params.S3_BUCKET}.s3-website-${params.AWS_REGION}.amazonaws.com/", returnStatus: true
                            return (r == 0);
                        }
                    }
                }
            }
        } 
    }
}

