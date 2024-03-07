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
            def version = sh(returnStdout: true, script: 'grep \'version=\' gradle.properties  | cut -d\'=\' -f2')
            buildName "${env.GIT_BRANCH.replace("origin/", "")}@${version}"
          }
        }
      }
      stage('Build') {
        steps {
          checkout scm
          sh './build.sh clean install'
        }
      }
      stage('Test') {
        steps {
          script {
//            try {
            sh './build.sh test'
//            } catch (err) {
//            }
          }
        }
      }
      stage('Publish') {
        steps {
          sh './build.sh publish'
        }
      }
    }
}

