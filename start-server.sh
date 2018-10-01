#!/bin/bash

export GRADLE_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
gradle server -Djenkins.httpPort=8082


