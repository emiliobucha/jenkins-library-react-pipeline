def upload(def artifact, def s3Artifact, def credentials, def region) {
    input "Upload Artifact ${artifact} to S3 Bucket ${s3Artifact}?"
    println artifact
    println s3Artifact
    withAWS(credentials: "${credentials.awsCredentials}", region: "${credentials.awsRegion}") {
        s3Upload(file:"${artifact}", bucket:"${s3Artifact}", path:"${artifact}")
    }
}

def uploadFolder(def folder, def s3Artifact, def credentials, def region) {
    input "Deploy Artifact ${config.artifact} to S3 Bucket ${config.s3Artifact}?"
    println artifact
    println s3Artifact
    withAWS(credentials: "${config.awsCredentials}", region: "${config.awsRegion}") {
        dir("${folder}") {
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