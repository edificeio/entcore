#!/usr/bin/env groovy

pipeline {
  agent any
    stages {
      stage('Build') {
        steps {
          checkout scm
          sh './build.sh clean install'
        }
      }
      stage('Test') {
        steps {
          sh './build.sh test'
        }
      }
      stage('Publish') {
        steps {
          sh './build.sh publish'
        }
      }
    }
}

