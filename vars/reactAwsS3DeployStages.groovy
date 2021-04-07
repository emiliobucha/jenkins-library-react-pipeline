def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

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