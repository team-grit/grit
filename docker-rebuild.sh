#!/bin/bash
./gradlew installApp
cp -rf build/install/GRIT docker 
docker build -t teamgrit/grit-docker:latest .
