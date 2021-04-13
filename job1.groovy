pipeline {
    agent any
    stages {
        stage ('test') {
            steps {
                echo "hello"
                echo env.TEST
                echo env.FOO
            }
        }
    }
}