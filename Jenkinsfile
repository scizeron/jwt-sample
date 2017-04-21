node {
   stage('Init') {
    def mvnHome = tool 'M3'
   }
   stage('Checkout') {
    checkout scm
   }
   stage ('Build') {
    sh "${mvnHome}/bin/mvn clean install"
   }
}