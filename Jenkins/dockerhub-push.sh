#!/usr/bin/env bash

# DockerHub Login
/home/ec2-user/docker-latest login -e ${dockerhub_mail} -u ${dockerhub_user} -p ${dockerhub_pwd}

#Tag images
/home/ec2-user/docker-latest -f moviedatabase_movies cdzwei/mvdb_movies:latest

# Push to DockerHub
/home/ec2-user/docker-latest push docker.io/cdzwei/mvdb_movies 
