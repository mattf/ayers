# Build Ayers

```mvn package -DskipTests```


# Download an image call it img.jpg


# Start https://github.com/rhdemo/imagescorer


# Run Ayers from the command line with local file

```java -Dlog4j.configurationFile=$PWD/log4j2.properties -jar target/ayers-1.0-SNAPSHOT.jar imageFile=img.jpg```


# Run Ayers from the command line with file in S3

```
source environment
curl -i -X PUT -H "X-Auth-Token: $S3_AWS_AUTH" http://$S3_AWS_HOST/v1/AUTH_gv0/ayers
curl -i -X PUT -H "X-Auth-Token: $S3_AWS_AUTH" -T img.jpg http://$S3_AWS_HOST/v1/AUTH_gv0/ayers/img.jpg
java -Dlog4j.configurationFile=$PWD/log4j2.properties -jar target/ayers-1.0-SNAPSHOT.jar "$(envsubst < swiftObj.json)"

```
