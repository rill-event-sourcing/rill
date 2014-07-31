*deployment

To deploy a custom capistrano script is provided.
With the makefiles in each project a tar or jar file is made and
uploaded to S3.
The CI server runs this on every build.
These builds can de deployed to the servers with the following
command:

./deploy.sh staging 7104c8d3f40dbb8ab897fdd2d0572c63224b13c6

For now staging is the only valid environment.
The SHA must be a full GIT SHA that was build previously.

The user running this script must have acces to the servers with a ssh key.
