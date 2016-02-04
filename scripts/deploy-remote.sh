#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower

cd ..

# Check if GLASSFISH_HOME is set
#if [ -z "$GLASSFISH_HOME" ]; then
#    echo "GLASSFISH_HOME is not set. Set it to the directory in which your glassfish instance is installed."
#    exit 1
#fi

# Copy the app folder to the right location
scp -r yo/app/ kitchen:/usr/local/glassfish/glassfish/domains/domain1/docroot/
# Copy the bower.json
scp yo/bower.json kitchen:/usr/local/glassfish/glassfish/domains/domain1/docroot/

#change directory to the glassfish docroot #install bower components
#Edit the index.html

ssh kitchen './deploy'

open -a firefox http://193.10.64.11:8080/app
