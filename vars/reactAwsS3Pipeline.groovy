#!/usr/bin/env groovy
def call(body) {


    def config = [:]
    config['GIT_CREDENTIALS'] = 'gitlab-semperti-gonzalo-acosta'
    config['PIPELINE_LIBRARY_REPOSITORY'] = 'https://gitlab.semperti.com/devops/jenkins-library.git'
    config['NPM_CMD'] = "npm"
    config['ENVIRONMENT'] = "development"
    config['AWS_REGION'] = "us-east-1"
    config['S3_BUCKET'] = "semperti-react-development-rapientrega"
    config['BUILD_FOLDER'] = "code/frontend/build"
    config['SOURCE_FRONTEND'] = "code/frontend"

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def vGIT_CREDENTIALS = env.GIT_CREDENTIALS == null ? config['GIT_CREDENTIALS'] : env.GIT_CREDENTIALS,
            vPIPELINE_LIBRARY_REPOSITORY = env.PIPELINE_LIBRARY_REPOSITORY == null ? config['PIPELINE_LIBRARY_REPOSITORY'] : env.PIPELINE_LIBRARY_REPOSITORY,
            vNPM_CMD = env.NPM_CMD == null ? config['NPM_CMD'] : env.NPM_CMD,
            vENVIRONMENT = env.ENVIRONMENT == null ? config['ENVIRONMENT'] : env.ENVIRONMENT,
            vAWS_REGION = env.AWS_REGION == null ? config['AWS_REGION'] : env.AWS_REGION,
            vS3_BUCKET = env.S3_BUCKET == null ? config['S3_BUCKET'] : env.S3_BUCKET,
            vBUILD_FOLDER =  env.BUILD_FOLDER == null ? config['BUILD_FOLDER'] : env.BUILD_FOLDER,
            vSOURCE_FRONTEND = env.SOURCE_FRONTEND == null ? config['SOURCE_FRONTEND'] : env.SOURCE_FRONTEND,
            vDEPLOY_JOB = env.DEPLOY_JOB == null ? config['DEPLOY_JOB'] : env.SOURCE_FRONTEND,
            vAWS_CREDENTIALS_ID = config['AWS_CREDENTIALS_ID'] == null ? 'aws-credentials-jenkins' : config['AWS_CREDENTIALS']



    pipeline {
        agent any

        options {
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            parallelsAlwaysFailFast()
            timestamps()
        }

        parameters {
            credentials( name: 'GIT_CREDENTIALS',  defaultValue: vGIT_CREDENTIALS, description: 'Credenciales para acceder a los repositorios.' )
            string(name: 'PIPELINE_LIBRARY_REPOSITORY', defaultValue: vPIPELINE_LIBRARY_REPOSITORY, description: 'Repositorio con librerias groovy.')
            string(name: 'NPM_CMD', defaultValue: vNPM_CMD, description: 'Comando NPM')
            string(name: 'S3_BUCKET', defaultValue: vS3_BUCKET, description: 'Bucket S3 de publicación')
            string(name: 'BUILD_FOLDER', defaultValue: vBUILD_FOLDER, description: 'Path relativo a la carpeta de compilación a publicar')
            string(name: 'AWS_REGION', defaultValue: vAWS_REGION, description: 'AWS Region')
            string(name: 'DEPLOY_JOB', defaultValue: vDEPLOY_JOB, description: 'Deploy Pipeline Job')
            string(name: 'SOURCE_FRONTEND', defaultValue: vSOURCE_FRONTEND, description: 'Path relativo del código del frontend')
            credentials(name: 'AWS_CREDENTIALS',  defaultValue: vAWS_CREDENTIALS_ID, description: 'Credenciales de AWS.' )
        }

        stages {
            stage('Checkout Source') {
                steps {
                    // git credentialsId: 'dc69dd47-d601-4cb0-adbe-548c17e15506', url: "http://<gitRepo>/<username>/<repoName>.git"
                    checkout scm
                    script {
                        dir(params.SOURCE_FRONTEND) {
                            npmVersion()
                        }
                    }
                }
            }
            stage('Install dependencies') {
                steps {
                    dir(params.SOURCE_FRONTEND) {
                        sh "${params.NPM_CMD} install"
                    }
                }
            }
            stage('Build React App Dev') {
                steps {
                    dir(params.SOURCE_FRONTEND){
                        sh "${params.NPM_CMD} run build"
                    }
                }
            }
            stage('Unit Tests') {
                steps {
                    dir(params.SOURCE_FRONTEND) {
                        echo 'Running Unit Tests'
                        sh "${params.NPM_CMD} test -- --watchAll=false"
                    }
                }
            }
            stage('Code Analysis') {
                steps {
                    script {
                        dir(params.SOURCE_FRONTEND) {
                            echo 'Running Code Analysis'
                        } 
                    }
                }
            }
            stage('Publish to Nexus') {
                steps {
                    echo 'Publish to Nexus'
                }
            }
            stage('Deploy Artifact to ElasticBeanstalk') {
                steps {
                    //input "Upload Artifact ${params.ENVIRONMENT} to Elastic Beanstalk?"
                    build job: "${params.DEPLOY_JOB}", parameters: [
                        string(name: 'AWS_REGION', value: params.AWS_REGION),
                        string(name: 'BUILD_FOLDER', value: params.BUILD_FOLDER),
                        string(name: 'S3_BUCKET', value: params.S3_BUCKET),
                        credentials(name: 'AWS_CREDENTIALS', value: params.AWS_CREDENTIALS_ID)
                    ], wait: false
                }
            }
            
        }
    }
}
