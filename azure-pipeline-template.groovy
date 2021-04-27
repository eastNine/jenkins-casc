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
      claimName: my-azurefile
  containers:
  - name: maven
    image: maven:3.3.9-jdk-8-alpine
    volumeMounts:
    - mountPath: "/root/.m2"
      name: maven-home
    command:
    - cat
    tty: true
  - name: azure-cli
    image: mcr.microsoft.com/azure-cli
    tty: true
    command:
    - cat
  - name: packer
    image: hashicorp/packer
    tty: true
    command:
    - cat
  - name: jq
    image: stedolan/jq
    tty: true
    command:
    - cat
""".stripIndent()
        }
    }
    parameters {
        string(name: 'OLD_VMSS', defaultValue: '')
        string(name: 'NEW_VMSS', defaultValue: '')
    }
    environment {
        RESOURCE_GROUP = 'dev-infratest'
        RG_APPGW = 'dev-infratest-agw'
        TARGET_APPGW = 'dev-infratest-agw-agw'
    }
    stages {
        stage('Build Artifact') {
            steps {
                container('maven') {
                    git url: env.GIT_REPO,
                    branch: env.GIT_BRANCH
                    sh 'ls -al'
                    sh "${BUILD_CMD}"
                    sh 'cp target/*.jar azscripts/${JOB_NAME}.jar'
                    //stash includes: 'azscripts/*', name: 'prjArtifact'
                }
            }
        }
        stage('Bake Azure Image') {
            steps {
                container('azure-cli') {
                    sh 'az login --identity'
                }
                container('packer') {
                    sh 'echo "Jenkins agent must have executable packer binary..."'
                    sh 'echo "Packer Version : " $(packer --version)'
                    //unstash 'prjArtifact'
                    sh 'ls -al azscripts'
                    sh '''
                        rm -f ./manifest.json
                        packer build -var-file="./azimage_arm.parameter.json" ./azimage_arm.json
                    '''
                }
                container('jq') {
                    sh '''
                        cat ./manifest.json
                        jq '.builds[].artifact_id' ./manifest.json
                    '''
                    script {
                        env.IMAGE_ID = sh(script: "jq '.builds[].artifact_id' ./manifest.json", , returnStdout: true).replace('"','').replace('/','\\/').trim()
                    }
                }
            }
        }
        stage('Deploy New VMSS') {
            steps {
                container('azure-cli') {
                    script {
                        // Get Latest(Blue) VMSS (1 item and order by name desc)
                        temp = sh(script: "az vmss list --resource-group ${RESOURCE_GROUP} --query \"[].name|[? contains(@, '${JOB_NAME}')].{name: @}|reverse(sort_by(@, &name))[:1].name\" --out tsv", returnStdout: true).trim()
                        env.OLD_VMSS = temp
                        env.NEW_VMSS = env.JOB_NAME + "-vmss-" + BUILD_NUMBER
                        // Get activate backend pool from Azure
                        temp = sh(script: "az network application-gateway rule show -g ${RG_APPGW} --gateway-name ${TARGET_APPGW} --name ${JOB_NAME}Rule --query 'backendAddressPool.id'", returnStdout: true).replace('"','').trim()
                        tempsplit = temp.split('/')
                        size = tempsplit.length
                        if (tempsplit[size-1] == env.JOB_NAME + "-" + 'blue')
                        {
                            env.TARGET_BACKEND = env.JOB_NAME + "-" + 'green'
                        }
                        else
                        {
                            env.TARGET_BACKEND = env.JOB_NAME + "-" + 'blue'
                        }
                    }
                    sh 'echo ${OLD_VMSS}'
                    sh 'echo ${TARGET_VMSS}'
                    sh 'echo This deployment will go to ${TARGET_BACKEND}'
                    sh 'echo ${IMAGE_ID}'
                    sh """
                        cp ./azuredeploy.parameter.json ./azuredeploy.parameter.json.ori
                        perl -pi -e 's/##APPGW_RG##/${RG_APPGW}/g' ./azuredeploy.parameter.json
                        perl -pi -e 's/##APPGW_NAME##/${TARGET_APPGW}/g' ./azuredeploy.parameter.json
                        perl -pi -e 's/##IMAGE_NAME##/${IMAGE_ID}/g' ./azuredeploy.parameter.json
                        perl -pi -e 's/##VMSS_NAME##/${NEW_VMSS}/g' ./azuredeploy.parameter.json
                        perl -pi -e 's/##BACKEND_POOL##/${TARGET_BACKEND}/g' ./azuredeploy.parameter.json
                    """
                    sh 'cat ./azuredeploy.parameter.json'
                    sh 'az login --identity'
                    sh 'echo "RUN az deployment"'
                    sh 'az deployment group create --resource-group ${RESOURCE_GROUP} --template-file azuredeploy.json --parameters @azuredeploy.parameter.json'
                }
            }
        }
        stage('VMSS Healthcheck & Switch Backendpool') {
            steps {
                container('azure-cli') {
                    sh """
                        #!/bin/bash
                        rm -f instanceIds.txt
                        az login --identity
                        az vmss list-instances --resource-group ${RESOURCE_GROUP} --name ${NEW_VMSS} --query "[].instanceId" >> instanceIds.txt
                    """
                    script {
                        def VMs = sh(script:"cat ./instanceIds.txt | jq '.|length'", returnStdout: true).toInteger()
                        num = 0
                        while(num < VMs) {
                            def VMID = sh(script:"cat ./instanceIds.txt | jq -r \".[${num}]\"", returnStdout: true).trim()
                            println VMID
                            def HSTATE = sh(script:"az vmss get-instance-view --resource-group ${RESOURCE_GROUP} --name ${NEW_VMSS} --instance-id ${VMID} --query \"vmHealth.status.code\"", returnStdout: true).replace('"','').trim()
                            println HSTATE
                            if (!HSTATE.equals("HealthState/healthy")) {
                                println "Healthcheck failed. Terminate new VMSS."
                                sh(script:"az vmss delete --resource-group ${RESOURCE_GROUP} --name ${NEW_VMSS} --no-wait")
                                IMAGE_ID = env.IMAGE_ID.replace('\\/','/')
                                sh(script:"az image delete --resource-group ${RESOURCE_GROUP} --ids ${IMAGE_ID}")
                                exit 1
                            }
                            num++
                        }
                    }
                    //sh 'az deployment group create --resource-group ${RESOURCE_GROUP} --template-file azuredeploy.switch.json --parameters @azuredeploy.parameter.json'
                    sh 'az network application-gateway rule update --address-pool ${TARGET_BACKEND} --gateway-name ${TARGET_APPGW} --name ${JOB_NAME}Rule --resource-group ${RG_APPGW}'
                }
            }
        }
        stage('Terminate Old VMSS') {
            steps {
                container('azure-cli') {
                    sh 'az login --identity'
                    // Backendpool health check
                    sh "az network application-gateway show-backend-health -g ${RG_APPGW} -n ${TARGET_APPGW} --query \"backendAddressPools[] | [? contains(backendAddressPool.id,'${JOB_NAME}')].backendHttpSettingsCollection[].servers[?health == 'Healthy'].health | [] | length(@)\""
                    sh 'echo ${OLD_VMSS}'
                    sh 'az vmss delete --resource-group ${RESOURCE_GROUP} --name ${OLD_VMSS} --no-wait'
                }
            }
        }
    }
}