#!/usr/bin/env groovy

pipeline {
  agent any

  environment {
    NPM_TOKEN = credentials('npm-token')
    TIPTAP_PRO_TOKEN = credentials('tiptap-pro-token')
    NPM_PUBLIC_TOKEN = credentials('npm-public-token')
  }

  stages {
    stage("Initialization") {
      when {
        environment name: 'RENAME_BUILDS', value: 'true'
      }
      steps {
        script {
          sh './build.sh $BUILD_SH_EXTRA_PARAM init'
          def version = sh(returnStdout: true, script: 'docker-compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout')
          buildName "${env.GIT_BRANCH.replace("origin/", "")}@${version}"
        }
      }
    }
    stage('Build') {
      steps {
        checkout scm
        sh './build.sh $BUILD_SH_EXTRA_PARAM init clean install'
      }
    }
    stage('Test') {
      steps {
        script {
          sh 'sleep 6'
/*            try {
            sh './build.sh $BUILD_SH_EXTRA_PARAM test'
          } catch (err) {
          }*/
        }
      }
    }
    stage('Publish') {
      steps {
        sh "NPM_TOKEN=$NPM_PUBLIC_TOKEN DRY_RUN=false ./build.sh \$BUILD_SH_EXTRA_PARAM publish"
      }
    }
    stage('Build image') {
      steps {
          sh './build.sh image'
      }
    }
  }
  post {
    cleanup {
      sh 'docker-compose down'
    }
  }
}

