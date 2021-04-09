def upload(def artifact, def s3Artifact, def credentials, def region) {
    input "Upload Artifact ${artifact} to S3 Bucket ${s3Artifact}?"
    println artifact
    println s3Artifact
    withAWS(credentials: "${credentials.awsCredentials}", region: "${credentials.awsRegion}") {
        s3Upload(file:"${artifact}", bucket:"${s3Artifact}", path:"${artifact}")
    }
}

def uploadFolder(def folder, def s3Artifact, def region) {
    input "Upload Artifact ${artifact} to S3 Bucket ${s3Artifact}?"
    println artifact
    println s3Artifact
    withAWS(credentials: "${credentials.awsCredentials}", region: "${credentials.awsRegion}") {
        s3Upload(file:"${artifact}", bucket:"${s3Artifact}", path:"${artifact}")
    }
}