#!/usr/bin/env groovy

pipeline {
  agent any
    stages {
      stage("Initialization") {
        when {
          environment name: 'RENAME_BUILDS', value: 'true'
        }
        steps {
          script {
            def version = sh(returnStdout: true, script: 'docker-compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout')
            buildName "${env.GIT_BRANCH.replace("origin/", "")}@${version}"
          }
        }
      }
      stage('Build') {
        steps {
          checkout scm
          sh 'GIT_BRANCH=develop-b2school ./build.sh $BUILD_SH_EXTRA_PARAM init clean install'
        }
      }
      stage('Test') {
        steps {
          script {
            sh 'sleep 6'
//            try {
//              sh 'GIT_BRANCH=develop-b2school ./build.sh $BUILD_SH_EXTRA_PARAM test'
//            } catch (err) {
//            }
          }
        }
      }
      stage('Publish') {
        steps {
          sh './build.sh $BUILD_SH_EXTRA_PARAM publish'
        }
      }
    }
  post {
    cleanup {
      sh 'docker-compose down'
    }
  }
}

