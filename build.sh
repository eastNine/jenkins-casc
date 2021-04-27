#!/bin/bash

# Always get a fresh image
docker pull jenkins/jenkins:lts-jdk11

docker build -t jenkins-casc:develop .