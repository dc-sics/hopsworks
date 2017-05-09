#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=25225
export WEBPORT=29309
export SERVER=bbc2.sics.se
export key=insecure_private_key
usr=fadi
basedir=/srv/hops/domains/domain1

scp ${usr}@${SERVER}:/home/${usr}/.vagrant.d/insecure_private_key .
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir} && sudo chown -R vagrant:vagrant docroot && sudo chmod -R 775 *"
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r /mnt/c/Users/Fadi/projects/hopsworks/hopsworks-web/yo/app/ vagrant@${SERVER}:${basedir}/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} /mnt/c/Users/Fadi/projects/hopsworks/hopsworks-web/yo/bower.json vagrant@${SERVER}:${basedir}/docroot/app
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && bower install"
#ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && perl -pi -e \"s/getApiLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks-api/api\/'/g\" scripts/services/RequestIn$
