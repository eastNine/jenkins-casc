pipeline {
    options { 
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    agent {
        kubernetes {
            yaml """\
apiVersion: v1
kind: Pod
metadata:
labels:
  some-label: some-label-value
spec:
  volumes:
  - name: maven-home
    persistentVolumeClaim:
      claimName: jenkins-maven-disk
  containers:
  - name: maven
    image: maven:alpine
    volumeMounts:
    - mountPath: "/root/.m2"
      name: maven-home
    command:
    - cat
    tty: true
  - name: busybox
    image: busybox
    command:
    - cat
    tty: true
""".stripIndent()
        }
    }
    stages {
        stage ('build') {
            steps {
                container('maven') {
                    git 'https://github.com/jenkinsci/kubernetes-plugin.git'
                    sh 'mvn -B clean'
                }
            }
        }
        stage ('run') {
            steps {
                container('busybox') {
                    sh 'echo HELLO K8S'
                }
            }
        }
    }
}