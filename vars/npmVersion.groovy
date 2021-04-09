def call() {
    def packageJSON = readJSON file: 'package.json'
    def packageJSONVersion = packageJSON.version
    devTag  = "${packageJSONVersion}-" + currentBuild.number
    prodTag = "${packageJSONVersion}"
    echo packageJSONVersion
    return packageJSONVersion
}