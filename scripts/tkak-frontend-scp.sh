#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=2222
export WEBPORT=8080
export SERVER=hopslocal
export key=/media/sf_Shared2/private_key

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/hops/domain1 && sudo chown -R glassfish:vagrant docroot && sudo chmod -R 775 *"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../yo/app/ vagrant@${SERVER}:/srv/hops/domain1/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../yo/bower.json vagrant@${SERVER}:/srv/hops/domain1/docroot/app

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/hops/domain1/docroot/app && bower install && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

#google-chrome -new-tab http://${SERVER}:$WEBPORT/app
