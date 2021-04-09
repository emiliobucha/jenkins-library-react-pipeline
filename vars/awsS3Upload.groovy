def upload(def artifact, def s3Artifact, def credentials, def region) {
    input "Upload Artifact ${artifact} to S3 Bucket ${s3Artifact}?"
    println artifact
    println s3Artifact
    withAWS(credentials: "${credentials}", region: "${region}") {
        s3Upload(file:"${artifact}", bucket:"${s3Artifact}", path:"${artifact}")
    }
}

def uploadFolder(def folder, def s3Artifact, def credentials, def region) {
    input "Deploy Artifact folder ${folder} to S3 Bucket ${s3Artifact}?"
    println s3Artifact
    withAWS(credentials: "${credentials}", region: "${region}") {
        dir("${folder}") {
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