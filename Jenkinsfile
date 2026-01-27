pipeline {
   agent any

    environment {
        clde_branch_master = "master"
        clde_branch_staging = "staging"
        gitCredentialId = "github jenkins access token"
        awsCredentialsID = "4387aab6-ff4e-44e9-9e15-eb1452a3870b"
    }

   tools {
        maven 'M3'
        jdk 'openjdk11'
    }

    options {
        buildDiscarder(
            logRotator(artifactNumToKeepStr: '3', numToKeepStr: '5')
        )
        disableConcurrentBuilds()
    }

   stages {
        stage('Get-Staging-Plugin') {
            when {
                branch "${clde_branch_staging}"
            }
            steps {
                git url:'https://github.com/cloudempiere/com.cloudempiere.ai.git',
                    credentialsId:"${gitCredentialId}",
                    branch:"${clde_branch_staging}"
                sh "sed -i -e 's+iDempiereCLDE+clde-server_staging-cloudempiere/iDempiereCLDE+g' com.cloudempiere.ai.parent/pom.xml "
                sh "sed -i -e 's+com.cloudempiere.core/com.cloudempiere.core.extensions.p2+com.cloudempiere.core_staging/com.cloudempiere.core.extensions.p2+g' com.cloudempiere.ai.parent/pom.xml "
                sh "sed -i -e 's+com.cloudempiere.aws/com.cloudempiere.aws.p2+com.cloudempiere.aws_staging/com.cloudempiere.aws.p2+g' com.cloudempiere.ai.parent/pom.xml "
            }
        }
        stage('Get-Prod-Plugin') {
            when {
                branch "${clde_branch_master}"
            }
            steps {
                git url:'https://github.com/cloudempiere/com.cloudempiere.ai.git',
                    credentialsId:"${gitCredentialId}",
                    branch:"${clde_branch_master}"
                sh "sed -i -e 's+iDempiereCLDE+clde-server_master-cloudempiere/iDempiereCLDE+g' com.cloudempiere.ai.parent/pom.xml "
                sh "sed -i -e 's+com.cloudempiere.core/com.cloudempiere.core.extensions.p2+com.cloudempiere.core_master/com.cloudempiere.core.extensions.p2+g' com.cloudempiere.ai.parent/pom.xml "
                sh "sed -i -e 's+com.cloudempiere.aws/com.cloudempiere.aws.p2+com.cloudempiere.aws_master/com.cloudempiere.aws.p2+g' com.cloudempiere.ai.parent/pom.xml "
            }
        }
        stage('Build') {
            when {
                    anyOf {
                        branch "${clde_branch_staging}"
                        branch "${clde_branch_master}"
                    }
                }
            steps {
                // Run Maven on a Unix agent.
                sh "mvn clean verify -U -DskipTests"
            }
        }

        stage('Publish Prod'){
            when {
                allOf {
                    branch "${clde_branch_master}"
                }
            }
            steps {
                withAWS(credentials:"${awsCredentialsID}", region:'eu-west-1') {
                    s3Upload(
                        file:"${WORKSPACE}/com.cloudempiere.ai.p2/target/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip", bucket:'cloudempiere-releases', path:"cloudempiere/production/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip"
                    )
                }
            }
        }

        stage('Publish Staging'){
            when {
                allOf {
                    branch "${clde_branch_staging}"
                }
            }
            steps {
                withAWS(credentials:"${awsCredentialsID}", region:'eu-west-1') {
                    s3Upload(
                        file:"${WORKSPACE}/com.cloudempiere.ai.p2/target/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip", bucket:'cloudempiere-releases', path:"cloudempiere/development/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip"
                    )
                }
            }
        }
   }
}
