node {
   stage('Checkout') {
    checkout scm
   }
   stage ('Build') {
    def mvnHome = tool 'M3'
    sh "${mvnHome}/bin/mvn -B -Dmaven.test.failure.ignore=true clean install"
   }
}