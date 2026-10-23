// Backend pipeline: build the jar, bake the image, restart only the backend container.
pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds(); buildDiscarder(logRotator(numToKeepStr: '20')) }
  triggers { pollSCM('H/5 * * * *') }
  environment {
    STACK = '/opt/artisticview'
    IMAGE = 'artisticview-service'
  }
  stages {
    stage('Build') {
      steps {
        sh 'chmod +x gradlew && ./gradlew build --no-daemon'
      }
    }
    stage('Image') {
      steps {
        sh 'docker build -t $IMAGE:$BUILD_NUMBER -t $IMAGE:latest .'
      }
    }
    stage('Deploy') {
      steps {
        // Keep the server's compose file in step with the repo, then roll the backend.
        sh '''
          cp deploy/docker-compose.yml $STACK/docker-compose.yml
          docker compose -f $STACK/docker-compose.yml --env-file $STACK/.env up -d --no-deps backend
        '''
      }
    }
    stage('Health') {
      steps {
        sh '''
          for i in $(seq 1 40); do
            status=$(docker inspect -f '{{.State.Health.Status}}' artisticview-backend 2>/dev/null || true)
            [ "$status" = healthy ] && echo "backend healthy" && exit 0
            sleep 5
          done
          docker logs --tail 80 artisticview-backend
          exit 1
        '''
      }
    }
  }
  post {
    always { sh 'docker image prune -f >/dev/null || true' }
  }
}
