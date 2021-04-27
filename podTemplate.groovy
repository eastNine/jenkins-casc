podTemplate(containers: [
    containerTemplate(name: 'azure-cli', image: 'mcr.microsoft.com/azure-cli', ttyEnabled: true, command: 'cat')
]) {
    node(POD_LABEL) {
        stage('Run Azure CLI') {
            container('azure-cli') {
                stage('Build a Maven project') {
                    sh 'echo HELLO'
                    sh 'az --version'
                    sh 'az login --identity'
                    sh 'az vmss list --resource-group dev-infratest'
                }
            }
        }
    }
}