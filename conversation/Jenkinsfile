#!/usr/bin/env groovy

pipeline {
  agent any

  stages {
    stage('Frontend') {
      steps {
        dir('frontend') {
          sh './build.sh clean init build'
        }
      }
    }
    
    stage('Backend') {
      steps {
        dir('backend') {
          sh 'mkdir -p ./src/main/resources/public/ || TRUE'
          sh 'rm -rf ./src/main/resources/public/*.js'
          sh 'rm -rf ./src/main/resources/public/*.css'
          sh 'cp -R ../frontend/dist/* ./src/main/resources/'
          sh 'mv ./src/main/resources/*.html ./src/main/resources/view'
          sh './build.sh clean build publish'
          sh 'rm -rf ../frontend/dist'
        }
      }
    }
  }
}