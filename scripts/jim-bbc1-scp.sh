#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=20005
export WEBPORT=14005
export SERVER=bbc1.sics.se
export key=private_key
usr=jdowling
basedir=/srv/hops/domain1
# /srv/hops/domain1/applications/hopsworks-web

scp ${usr}@bbc1.sics.se:/home/${usr}/hopsworks-chef/.vagrant/machines/default/virtualbox/private_key .
./.vagrant/machines/default/virtualbox/private_key
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir} && sudo chown -R glassfish:vagrant docroot && sudo chmod -R 775 *"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../yo/app/ vagrant@${SERVER}:${basedir}/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../yo/bower.json vagrant@${SERVER}:${basedir}/docroot/app

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && bower install && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://${SERVER}:$WEBPORT/app
