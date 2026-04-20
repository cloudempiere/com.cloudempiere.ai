pipeline {
    agent any

    parameters {
        choice(name: 'REPO_SOURCE', choices: ['Default', 'S3', 'Local'],
               description: 'P2 repo source. Default = S3 for staging/master, Local for other branches')
    }

    environment {
        clde_branch_master = "master"
        clde_branch_staging = "staging"
        gitCredentialId = "github jenkins access token"
        awsCredentialsID = "4387aab6-ff4e-44e9-9e15-eb1452a3870b"

        // S3 configuration
        S3_BUCKET = 'cloudempiere-releases'

        // S3 ZIP artifact names (iDempiere core always uses local workspace)
        CLDE_CORE_ZIP = 'com.cloudempiere.core.extensions.p2-0.1.0-SNAPSHOT.zip'
        CLDE_AWS_ZIP = 'com.cloudempiere.aws.p2-1.3.0-SNAPSHOT.zip'
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
        stage('Resolve Repo Source') {
            steps {
                script {
                    def isCiBranch = (env.BRANCH_NAME == clde_branch_staging || env.BRANCH_NAME == clde_branch_master)
                    def useS3 = (params.REPO_SOURCE == 'S3') || (params.REPO_SOURCE == 'Default' && isCiBranch)
                    env.EFFECTIVE_SOURCE = useS3 ? 'S3' : 'Local'

                    if (env.BRANCH_NAME == clde_branch_master) {
                        env.BRANCH_SUFFIX = 'master'
                        env.S3_ENV_PATH = 'cloudempiere/production'
                    } else {
                        env.S3_ENV_PATH = 'cloudempiere/development'
                        env.BRANCH_SUFFIX = 'staging'
                    }

                    echo "Effective repo source: ${env.EFFECTIVE_SOURCE}"
                    echo "Branch suffix: ${env.BRANCH_SUFFIX}"
                    if (env.EFFECTIVE_SOURCE == 'S3') {
                        echo "S3 path: ${env.S3_ENV_PATH}"
                    }
                }
            }
        }

        stage('Download Dependencies') {
            when {
                expression { env.EFFECTIVE_SOURCE == 'S3' }
            }
            steps {
                script {
                    sh "rm -rf deps"

                    def s3Path = env.S3_ENV_PATH
                    def artifacts = [
                        [zip: CLDE_CORE_ZIP, dir: 'clde-core'],
                        [zip: CLDE_AWS_ZIP,  dir: 'clde-aws']
                    ]

                    sh "mkdir -p deps"

                    artifacts.each { artifact ->
                        echo "Downloading ${artifact.zip} from ${s3Path}..."

                        withAWS(credentials: awsCredentialsID, region: 'eu-west-1') {
                            s3Download(
                                file: "deps/${artifact.zip}",
                                bucket: S3_BUCKET,
                                path: "${s3Path}/${artifact.zip}",
                                force: true
                            )
                        }

                        sh """
                            mkdir -p deps/${artifact.dir}
                            unzip -q -o deps/${artifact.zip} -d deps/${artifact.dir}
                        """

                        echo "Downloaded and extracted: ${artifact.zip} -> deps/${artifact.dir}"
                    }
                }
            }
        }

        stage('Set Repository URLs') {
            steps {
                script {
                    def suffix = env.BRANCH_SUFFIX
                    def base = "${WORKSPACE}/.."

                    // iDempiere core always uses local sibling workspace
                    env.IDEMPIERE_CORE_REPO = "file://${base}/clde-server_${suffix}-cloudempiere/iDempiereCLDE/core/org.idempiere.p2/target/repository"

                    if (env.EFFECTIVE_SOURCE == 'S3') {
                        env.CLDE_CORE_REPO = "file://${WORKSPACE}/deps/clde-core"
                        env.CLDE_AWS_REPO  = "file://${WORKSPACE}/deps/clde-aws"
                    } else {
                        env.CLDE_CORE_REPO = "file://${base}/com.cloudempiere.core_${suffix}/com.cloudempiere.core.extensions.p2/target/repository"
                        env.CLDE_AWS_REPO  = "file://${base}/com.cloudempiere.aws_${suffix}/com.cloudempiere.aws.p2/target/repository"
                    }

                    echo "Repository URLs:"
                    echo "  iDempiere core:    ${env.IDEMPIERE_CORE_REPO}"
                    echo "  CloudEmpiere core: ${env.CLDE_CORE_REPO}"
                    echo "  AWS:               ${env.CLDE_AWS_REPO}"
                }
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
                sh """
                    mvn clean verify -U -DskipTests \
                        -Didempiere.core.repository.url=${IDEMPIERE_CORE_REPO} \
                        -Dcloudempiere.core.repository.url=${CLDE_CORE_REPO} \
                        -Dcloudempiere.aws.repository.url=${CLDE_AWS_REPO}
                """
            }
        }

        stage('Publish Prod') {
            when {
                allOf {
                    branch "${clde_branch_master}"
                }
            }
            steps {
                withAWS(credentials: "${awsCredentialsID}", region: 'eu-west-1') {
                    s3Upload(
                        file: "${WORKSPACE}/com.cloudempiere.ai.p2/target/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip",
                        bucket: 'cloudempiere-releases',
                        path: "cloudempiere/production/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip"
                    )
                }
            }
        }

        stage('Publish Staging') {
            when {
                allOf {
                    branch "${clde_branch_staging}"
                }
            }
            steps {
                withAWS(credentials: "${awsCredentialsID}", region: 'eu-west-1') {
                    s3Upload(
                        file: "${WORKSPACE}/com.cloudempiere.ai.p2/target/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip",
                        bucket: 'cloudempiere-releases',
                        path: "cloudempiere/development/com.cloudempiere.ai.p2-10.0.2-SNAPSHOT.zip"
                    )
                }
            }
        }
    }
}
