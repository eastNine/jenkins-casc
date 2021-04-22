pipeline {
    pod {
        containers {
            container {
                name 'maven'
                image 'maven:3.3.9-jdk-8-alpine'
            }
            container {
                name 'golang'
                image 'golang:1.8.0'
            }
            container {
                name 'azure-cli'
                image 'mcr.microsoft.com/azure-cli'
            }
        }
    }
    agent none
    stages {
        stage ('build') {
            agent {
                container {
                    name 'maven'
                }
            }
            steps {
                git 'https://github.com/jenkinsci/kubernetes-plugin.git'
                sh 'mvn -B clean install'
            }
        }
        stage ('run') {
            agent {
                container {
                    name 'azure-cli'
                }
            }
            steps {
                sh 'az login --identity'
            }
        }
    }
}