import com.cloudbees.plugins.credentials.CredentialsProvider
import groovy.json.JsonSlurper
import hudson.util.Secret
import java.io.File
import java.net.URI
import java.nio.file.Paths
import jenkins.model.Jenkins
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
 
def verboseEcho(msg) {
    if (params.verbose) {
        println(msg)
    }
}
 
def refreshArtifactoryToken(accessToken, refreshToken) {
    final String refreshURL = 'https://artifacthub-phx.oci.oraclecorp.com/api/security/token'
 
    def curlOutput = sh (
        script: "curl -X POST '${refreshURL}' -H 'accept: application/json' -d 'grant_type=refresh_token' "
              + "-d 'expires_in=7776000' -d 'access_token=${accessToken}' -d 'refresh_token=${refreshToken}'",
        returnStdout: true
    ).trim()
 
    verboseEcho('Refresh Response : \n' + curlOutput)
 
    def refreshResponse
    def jsonSlurper = new JsonSlurper()
    try {
        refreshResponse = jsonSlurper.parseText(curlOutput)
    } catch (e) {
        throw new hudson.AbortException('Failed to parse response: ' + e)
    }
 
    if (refreshResponse.error) {
        throw new hudson.AbortException('Token refresh failed.  Response: \n' + curlOutput)
    }
 
    println('Token refresh Succeeded')
 
    def newAcessToken
    def newRefreshToken
    if (refreshResponse.access_token?.trim() && refreshResponse.refresh_token?.trim()) {
 
        newAcessToken = Secret.fromString(refreshResponse.access_token)
        verboseEcho('New Access Token : ' + newAcessToken.toString())
 
        newRefreshToken = Secret.fromString(refreshResponse.refresh_token)
        verboseEcho('New Refresh Token: ' + newRefreshToken.toString())
 
    } else {
        throw new hudson.AbortException('Access Token and/or Refresh Token not set - cannot continue')
    }
 
    return [newAcessToken, newRefreshToken]
}
 
def getJenkinsCredential(id) {
 
    def creds = CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.Credentials.class,
        Jenkins.instance,
        null,
        null
    );
 
    cred = creds.find { it.id == id}
    verboseEcho("Id          : " + cred.getId())
    verboseEcho("Description : " + cred.getDescription())
    verboseEcho("Secret      : " + cred.getSecret().toString())
 
    return cred
}
 
def getJenkinsCredentialSecret(id) {
    return getJenkinsCredential(id).getSecret()
}
 
def updateJenkinsCredential(id, token) {
 
    cred = getJenkinsCredential(id)
 
    credentials_store = Jenkins.instance.getExtensionList(
                'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
                )[0].getStore()
 
    updated = credentials_store.updateCredentials(
                com.cloudbees.plugins.credentials.domains.Domain.global(), 
                cred,
                new StringCredentialsImpl(cred.scope, cred.id, cred.description, token)
                )
 
    if (updated) {
        println('Password updated for ' + cred.id)
    } else {
        throw new hudson.AbortException('Failed to update Jenkins secret for ' + cred.id)
    }
}
 
// withTimeout(time: 15, unit: 'MINUTES') {
 
//     def access_token_id = params.access_token_id
//     def refresh_token_id = params.refresh_token_id
 
//     stage 'Token Refresh', {
 
//         // if the parameters are set
//         if (access_token_id?.trim() && refresh_token_id?.trim()) {
 
//             // Get the current tokens from Jenkins Credential Store
//             def currentAccessToken = getJenkinsCredentialSecret(access_token_id)
//             def currentRefreshToken = getJenkinsCredentialSecret(refresh_token_id)
 
//             // Refresh the Artifactory Token
//             def (newAccessToken, newRefreshToken) = refreshArtifactoryToken(currentAccessToken, currentRefreshToken)
 
//             // If set update the Jenkins Credential Store
//             if (newAccessToken && newRefreshToken) {
//                 updateJenkinsCredential(access_token_id, newAccessToken)
//                 updateJenkinsCredential(refresh_token_id, newRefreshToken)
//             } else {
//                 throw new hudson.AbortException('The Artifactory Token refresh failed')
//             }
 
//         } else {
//             throw new hudson.AbortException('This job must supply parameters for access_token_id and refresh_token_id')
//         }
//     }
// }

pipeline{

    options {
        timeout(time: 15, unit: 'MINUTES')
    }

    environment {
        HTTP_PROXY  = 'http://www-proxy-hqdc.us.oracle.com:80'
        HTTPS_PROXY = 'http://www-proxy-hqdc.us.oracle.com:80'
        NO_PROXY    = 'artifacthub-phx.oci.oraclecorp.com,.us.oracle.com,.oraclecorp.com,localhost,127.*.*.*'
        GECKODRIVER_SKIP_DOWNLOAD = 'true'
    }

    agent {
        kubernetes {
            label 'k8s-agent-large-mem-oci-ol8'
        }
    }

    parameters {
        string(name: 'access_token_id', defaultValue: 'artifactory_access_token', description: 'The Jenkins credential ID storing the access token to Artifactory')
        string(name: 'refresh_token_id', defaultValue: 'artifactory_refresh_token', description: 'The Jenkins credential ID storing the refresh token to Artifactory')
        booleanParam(name: 'verbose', defaultValue: true, description: 'Check for enabling logging')
    }

    stages {
        stage ('Token Refresh') {
            steps {
                script {

                    println "Starting the Token refresh stage"

                    def access_token_id = params.access_token_id
                    def refresh_token_id = params.refresh_token_id

                    if (access_token_id?.trim() && refresh_token_id?.trim()) {
            
                        // // Get the current tokens from Jenkins Credential Store
                        def currentAccessToken = getJenkinsCredentialSecret(access_token_id)
                        def currentRefreshToken = getJenkinsCredentialSecret(refresh_token_id)
            
                        // // Refresh the Artifactory Token
                        // def (newAccessToken, newRefreshToken) = refreshArtifactoryToken(currentAccessToken, currentRefreshToken)
            
                        // // If set update the Jenkins Credential Store
                        // if (newAccessToken && newRefreshToken) {
                        //     updateJenkinsCredential(access_token_id, newAccessToken)
                        //     updateJenkinsCredential(refresh_token_id, newRefreshToken)
                        // } else {
                        //     throw new hudson.AbortException('The Artifactory Token refresh failed')
                        // }

                            println "Inside the if Block"
                            println "Current Access Token: ${currentAccessToken}"
                            println "Current Refresh Token: ${currentRefreshToken}"
                            println "terminating pipeline job successfully"
            
                    } else {
                        throw new hudson.AbortException('This job must supply parameters for access_token_id and refresh_token_id')
                    }
                }
            }
        }
    }
}