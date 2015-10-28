#!/usr/bin/env bash

# DockerHub Login
docker login -e ${dockerhub_mail} -u ${dockerhub_user} -p ${dockerhub_pwd}

#Tag images
docker tag -f moviedatabase_movies cdzwei/mvdb_movies:latest

# Push to DockerHub
docker push docker.io/cdzwei/mvdb_movies 
