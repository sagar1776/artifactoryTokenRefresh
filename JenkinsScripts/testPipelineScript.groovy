pipeline {
    options {
        timeout(time: 15, unit: 'MINUTES')
    }

   agent {
      kubernetes {
          label 'k8s-agent-large-mem-oci-ol8'
      }
    }

    stages {
        stage('Use Secret Text') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'artifactory_access_token', variable: 'SECRET_TEXT')]) {
                        // Use the secret directly within this block
                        sh "echo Using secret: \$SECRET_TEXT"
                        
                        // If you absolutely need to write it to a file (not recommended):
                        sh "echo \$SECRET_TEXT > artifactory_access_token.key"
                        sh "cat artifactory_access_token.key"
                        sh "rm artifactory_access_token.key"  // Clean up immediately
                    }
                }
            }
        }
    }
}
