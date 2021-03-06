FROM jenkins/jenkins:lts-jdk11

# copy the list of plugins we want to install
COPY plugins.txt /usr/share/jenkins/plugins.txt

# run the install-plugins script to install the plugins ! will be deprecated
#RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/plugins.txt
# modern plugin cli
#RUN jenkins-plugin-cli --plugins kubernetes workflow-aggregator git configuration-as-code
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/plugins.txt

# disable the setup wizard as we will set up jenkins as code :)
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

# copy the seedjob and job files into the image
# COPY seedjob.groovy /usr/local/seedjob.groovy
# COPY job1.groovy /usr/local/job1.groovy
# COPY job2.groovy /usr/local/job2.groovy
# COPY template1.groovy /usr/local/template1.groovy
# COPY k8sjob.groovy /usr/local/k8sjob.groovy
# COPY spring-petclinic.groovy /usr/local/spring-petclinic.groovy
COPY *.groovy /usr/local/
# copy the config-as-code yaml file into the image
COPY jenkins-casc.yaml /usr/local/jenkins-casc.yaml
# tell the jenkins config-as-code plugin where to find the yaml file
ENV CASC_JENKINS_CONFIG /usr/local/jenkins-casc.yaml