# Deployment to ubuntu server (e.g. through digital ocean)

### Provision
- Provision an ubuntu server machine (can be a cloud server such as digital ocean)
- A machine with 1gb RAM and 30gb hard disk has been sufficient for early tests
- Enable connection to the box via ssh - [how to](https://www.digitalocean.com/community/tutorials/how-to-set-up-ssh-keys--2) 

### Setup Twitter API for app: 
*(Optional: use as many login methods as you like)*

- Create new app on www.dev.twitter.com
- Set callback url to be `https://[SERVER URL]/twitter-callback`
- Get consumer API key and secret

### Stonecutter set up: 
*(Optional: use as many login methods as you like)*

- Deploy a Stonecutter instance, as found [here](https://github.com/d-cent/stonecutter)
- Login as an admin
- Navigate to Apps page
- Add an app for your Objective8 instance
- Get client ID and secret, as generated by Stonecutter 

### Facebook set up: 
*(Optional: use as many login methods as you like)*

- Create new website app [here](https://developers.facebook.com)
- Navigate to settings > advanced > Client OAuth Settings > Valid OAuth redirect URIs
- Enter `http://[SERVER URL]/facebook-callback`
- Save changes
- Take the app out of development mode
- Get consumer API key and secret

### Coracle set up: 
*(Optional: needed if deploying alongside [Mooncake](https://github.com/d-cent/mooncake))*

- Deploy Coracle instance, as found [here](https://github.com/d-cent/coracle)
- If you are not using coracle, remove the coracle role in the *ops/digital_ocean_box_playbook.yml* file

### Amazon Web Services set up:
*(Optional: required if you want to back up the database)*

- Create an AWS account [here](https://aws.amazon.com/)
- Get your Access Key ID and Secret Access Key as described [here](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html)

### Configure with ansible
- Install Ansible
- In file *ops/digital_ocean_box.inventory* replace:
    - `ansible_ssh_host` with the IP address of your ubuntu server machine
    - `site_address` with the URL of the server
    - `external_jwk_set_url` with the URL of the server followed by /as2/jwk-set
- Use the *objective8_config_template* found in the */ops* directory and either replace the empty strings with your credentials or delete the variable. Save it for use in the next step and take note of the file path. You can find more information about the configuration variables [here](./CONFIG.md).
- Remove from */ops/roles/objective8_application_config/templates/objective8_config.j2* the variables that you deleted in the previous step.
- Create a */ops/roles/nginx/files/secure/* directory, and copy your SSL certificate and key files there, with the names *objective8.key* and *objective8.crt*.

Run Ansible playbook:

  The following command will install necessary packages and configure them (it will take a few minutes).
  It will require choosing a database password and supplying your Amazon S3 credentials for automatically backing up (encrypted) the database to an Amazon S3 bucket: 
  ```
  ansible-playbook ops/digital_ocean_box_playbook.yml -i ops/digital_ocean_box.inventory --extra-vars "CONFIG_FILE_PATH={config file path from the previous step without the curly braces}"
  ```
  
### Deploy application to the server

  The following will copy the application to the server and start it running as a service in a docker container.
  Once complete you should be able to access the app at your IP address.

  ```
  chmod +x deploy_prod.sh
  REMOTE_USER={username on server} SERVER_IP={IP address of server} ./deploy_prod.sh
  ```
