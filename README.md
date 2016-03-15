# HopsWorks
HopsWorks Big data Management Platform

#### Build Requirements (for Ubuntu)
### Clone repository
```
git clone https://github.com/hopshadoop/hopsworks.git
```

###Install curl
```
sudo apt-get install curl
```

###Install nodejs
```
curl -sL https://deb.nodesource.com/setup_5.x | sudo -E bash -
```

```
sudo apt-get install -y nodejs
```

###Install bower globally
```
sudo npm install -g bower
```

###Install grunt globally
```
sudo npm install -g grunt-cli
```

###Change to 'hopsworks/yo' directory
```
cd hopsworks/yo
```

###Install dependencies listed in package.json
```
sudo npm install
```

###Install maven
```
sudo apt-get install maven
```

###Change file ownership of hopswork to local user
```
cd ../../
sudo chown -R yourUser:yourUser hopsworks/
```

###Change into the hopsworks directory and build with maven
```
cd hopsworks/
mvn install
```

#### Maven
Maven uses yeoman-maven-plugin to build both the frontend and the backend.
Maven first executes the Gruntfile in the yo directory, then builds the back-end in Java.
The yeoman-maven-plugin copies the dist folder produced by grunt from the yo directory to the target folder of the backend.
Both the frontend and backend are packaged together in a single war file.

To access the admin page go to index.xhtml.


#### Front-end Development Tips

The javascript produced by building maven is obsfuscated. For debugging javascript, we recommend that you use the following script
to deploy changes to HTML or javascript:

```
cd scripts
./dev-deploy-frontend.sh
```
