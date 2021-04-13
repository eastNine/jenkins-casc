pipelineJob('job2') {
    definition {
        environmentVariables {
          envs(WORLD: 'world')
        }
        cps {
            File file = new File('/usr/local/template1.groovy')
            String filebody = file.getText('UTF-8')
            script(filebody)
            sandbox()
        }
    }
}