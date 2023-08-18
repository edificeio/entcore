#!/usr/bin/env groovy

pipeline {
  agent any
    stages {
      stage('Build') {
        steps {
          checkout scm
          sh './build.sh --no-user clean install'
        }
      }
      stage('Test') {
        steps {
          script {
//            try {
            sh './build.sh --no-user test'
//            } catch (err) {
//            }
          }
        }
      }
      stage('Publish') {
        steps {
          sh './build.sh --no-user publish'
        }
      }
    }
}

