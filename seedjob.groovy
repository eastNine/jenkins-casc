// create an array with our two pipelines
pipelines = ["first-pipeline", "another-pipeline"]
// iterate through the array and call the create_pipeline method
pipelines.each { pipeline ->
    println "Creating pipeline ${pipeline}"
    create_pipeline(pipeline)
}
// a method that creates a basic pipeline with the given parameter name
def create_pipeline(String name) {
    pipelineJob(name) {
        definition {
            cps {
                sandbox(true)
                script("""
// this is an example declarative pipeline that says hello and goodbye
pipeline {
    agent any
    stages {
        stage("Hello") {
            steps {
                echo "Hello from pipeline ${name}"
            }
        }
        stage("Goodbye") {
            steps {
                echo "Goodbye from pipeline ${name}"
            }
        }
    }
}
""")
            }
        }
    }
}

pipelines = ["k8sjob", "spring-petclinic"]
pipelines.each { pipeline ->
    println "Creating pipeline ${pipeline}"
    create_k8spipeline(pipeline)
}
def create_k8spipeline(String pipelineName) {
    pipelineJob(pipelineName) {
        definition {
            cps {
                sandbox(true)
                File file = new File('/usr/local/' + pipelineName + '.groovy')
                String filebody = file.getText('UTF-8')
                script(filebody)
            }
        }
    }
}