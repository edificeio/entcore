#!/usr/bin/env groovy

pipeline {
  agent any
    stages {
      stage('Build') {
        steps {
          checkout scm
          sh './build.sh $BUILD_SH_EXTRA_PARAM clean install'
        }
      }
      stage('Test') {
        steps {
          script {
//            try {
            sh './build.sh $BUILD_SH_EXTRA_PARAM test'
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
}

