#!/usr/bin/env bash

cd ../movie-database-movies

# Docker Build Movies
sudo /home/ec2-user/docker-latest build --tag="cdzwei/mvdb_movies" .
