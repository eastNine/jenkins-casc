#!/bin/bash

# run below command before run this script
# docker login infraregistry.azurecr.io

docker tag jenkins-casc:develop infraregistry.azurecr.io/jenkins-casc:develop
docker push infraregistry.azurecr.io/jenkins-casc:develop