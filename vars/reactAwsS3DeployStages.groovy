#!/usr/bin/env groovy
def awsCredentials = [[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-credentials-jenkins']]
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
        credentials(defaultValue: awsCredentials, description: 'AWS credentials id.', name: 'AWS_CREDENTIALS')
    }
    stages{
        stage('Initialize AWS Credentials'){
            steps {
                script {
                    awsCredentials = [[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: params.AWS_CREDENTIALS]]
                }
            }
        }
        stage('Deploy Artifact to S3 Bucket') {
                steps {
                    script{
                        awsS3Upload.uploadFolder(params.BUILD_FOLDER, params.S3_BUCKET, params.AWS_CREDENTIALS, params.AWS_REGION)
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

