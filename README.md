# Build

```mvn package```

# Deploy on OpenWhisk

```
mvn package
npm install
export __OW_API_KEY=$(wsk property get --auth | head -n1 | awk '{print $3}’)
./deploy # Before running check the location of the node binary
./verify
```

# Run from the command line

## Start https://github.com/rhdemo/imagescorer

## Invoke with an image from unsplash

```java -jar target/ayers-1.0-SNAPSHOT.jar "$(cat args.json)"```


# Running in OpenWhisk

## Create the action

First we need to build the jar file and create an OpenWhisk action
from it.

```
source environment
mvn clean test package
wsk -i action update ayers target/ayers-1.0-SNAPSHOT.jar --main com.redhat.summit2018.Action --web=true -p modelEndpoint $MODEL_ENDPOINT
```

## Invoke the action manually

Then we can test the action manually, passing a Red Hat Shadowman logo
image to the scoring engine which helpfully scores it as a person.

```
wsk -i action invoke ayers --blocking -p swiftObj '{"container": "img", "method": "PUT", "object": "logo.png", "token": "BOGUS", "url": "https://www.redhat.com/profiles/rh/themes/redhatdotcom/"}'
wsk -i activation logs <activation_id>
```

## Create and invoke a trigger and rule for the action

After firing the trigger, we list the activations. You may have to
issue that command a couple of times until you see the ayersTrigger
and ayers activations show up.

```
wsk -i trigger create ayersTrigger
wsk -i rule create ayersRule ayersTrigger ayers
wsk -i trigger fire ayersTrigger -p swiftObj '{"container": "img", "method": "PUT", "object": "logo.png", "token": "BOGUS", "url": "https://www.redhat.com/profiles/rh/themes/redhatdotcom/"}'
wsk -i activation list
```
## Trigger URL to fire from a Gluster webhook

The details of setting up the Gluster webhook are out of scope for
this, but you can see the trigger URL by adding the `-v` parameter to
the `wsk -i trigger fire` command. That will show you both the
authorization HTTP header needed as well as the URL to POST to.

The general trigger URL format is `https://<openwhisk apihost>/api/v1/namespaces/_/triggers/ayersTrigger`

For example:

```
wsk -i -v trigger fire ayersTrigger -p swiftObj '{"container": "img", "method": "PUT", "object": "logo.png", "token": "BOGUS", "url": "https://www.redhat.com/profiles/rh/themes/redhatdotcom/"}'
REQUEST:
[POST]	https://openwhisk-openwhisk.apps.summit-aws.sysdeseng.com/api/v1/namespaces/_/triggers/ayersTrigger
Req Headers
{
  "Authorization": [
    "Basic <base64 encoded auth shows here>"
  ],
  "Content-Type": [
    "application/json"
  ],
  "User-Agent": [
    "OpenWhisk-CLI/1.0 (2018-03-09T02:23:55.750+0000)"
  ]
}
...
```
