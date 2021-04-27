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

pipelines = ["k8sjob"]
repos     = ["test"]
branches  = ["master"]
build_cmds = ["mvn clean package"]
idx = 0
pipelines.each { pipeline ->
    println "Creating pipeline ${pipeline}"
    create_k8spipeline(pipeline, repos[idx], branches[idx], build_cmds[idx])
    idx++
}
def create_k8spipeline(String pipelineName, String repo, String branch, String build_cmd) {
    pipelineJob(pipelineName) {
        definition {
            environmentVariables {
                envs(GIT_REPO: repo,
                    GIT_BRANCH: branch,
                    BUILD_CMD: build_cmd
                )
            }
            cps {
                sandbox(true)
                File file = new File('/usr/local/' + pipelineName + '.groovy')
                String filebody = file.getText('UTF-8')
                script(filebody)
            }
        }
    }
}

pipelines = ["spring-petclinic"]
repos     = ["https://github.com/eastNine/spring-petclinic.git"]
branches  = ["main"]
build_cmds = ["mvn clean package"]
idx = 0
pipelines.each { pipeline ->
    println "Creating pipeline ${pipeline}"
    create_k8s_template_pipeline(pipeline, repos[idx], branches[idx], build_cmds[idx])
    idx++
}
def create_k8s_template_pipeline(String pipelineName, String repo, String branch, String build_cmd) {
    pipelineJob(pipelineName) {
        definition {
            environmentVariables {
                envs(GIT_REPO: repo,
                    GIT_BRANCH: branch,
                    BUILD_CMD: build_cmd
                )
            }
            cps {
                sandbox(true)
                File file = new File('/usr/local/azure-pipeline-template.groovy')
                String filebody = file.getText('UTF-8')
                script(filebody)
            }
        }
    }
}