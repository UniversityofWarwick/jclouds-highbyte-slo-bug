# JClouds multipart SLO Swift bug

This repository demonstrates an apparent bug in JClouds when PUTing a multipart static large object (SLO) to Openstack
Swift, where the key for that blob contains high byte characters - the Content-Length for the manifest JSON file is set
to the length of the String, but the high byte unicode characters are 2-byte sequences (in this example) so the content
length sent is too small, leading to an exception.

## Steps to reproduce

### Run a Swift installation (e.g. with Devstack)

We used [a Vagrant devstack install](https://github.com/openstack-dev/devstack-vagrant) with the default config. We had
to make one change due to the latest Devstack not supporting Ubuntu trusty (at the time of writing) to set FORCE=true in
the Vagrantfile before provisioning.

    diff --git a/Vagrantfile b/Vagrantfile
    index 1806543..6b102cd 100644
    --- a/Vagrantfile
    +++ b/Vagrantfile
    @@ -78,7 +78,7 @@ def configure_vm(name, vm, conf)

       if conf['setup_mode'] == "devstack"
         vm.provision "shell" do |shell|
    -      shell.inline = "sudo su - stack -c 'cd ~/devstack && ./stack.sh'"
    +      shell.inline = "sudo su - stack -c 'cd ~/devstack && FORCE=yes ./stack.sh'"
         end
       end

Our sample config.yaml (setting passwords and enabling Swift at the bottom):

    hostname_manager: manager.yoursite.com
    hostname_compute: compute.yoursite.com

    user_domains: .yoursite.com

    stack_password: secretadmin
    service_password: secretadmin
    admin_password: secretadmin

    stack_sshkey:

    setup_mode: devstack

    bridge_int: eth1

    manager_extra_services: s-proxy s-object s-container s-account

### Set Swift authentication properties

You can edit `src/test/resources/swift.properties` to put the correct credentials in before running the tests.

### Run the tests

Run `./gradlew test`. This will run the tests for a transient, openstack-swift and filesystem blob store - you should
see that the only test that fails is `SwiftJCloudsSLOTest.sloWithHighByteChars` - this will fail with an error at the final
stage of the SLO upload because the upload of the manifest file will fail.

## Sample output

This test uses a key of `Nişan.rar` containing the Unicode character 'LATIN SMALL LETTER S WITH CEDILLA' (U+015F)

    12396 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test/Ni%C5%9Fan.rar/slo/1491214092.299000/38547913/0/00000001 HTTP/1.1
    ...
    12396 DEBUG jclouds.headers >> Content-Type: application/unknown
    12397 DEBUG jclouds.headers >> Content-Length: 33554432
    12397 DEBUG jclouds.headers >> Content-Disposition: Nişan.rar
    12666 DEBUG jclouds.headers << HTTP/1.1 201 Created
    ...
    14604 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test/Ni%C5%9Fan.rar/slo/1491214092.299000/38547913/0/00000002 HTTP/1.1
    ...
    14604 DEBUG jclouds.headers >> Content-Type: application/unknown
    14604 DEBUG jclouds.headers >> Content-Length: 4993481
    14604 DEBUG jclouds.headers >> Content-Disposition: Nişan.rar
    14643 DEBUG jclouds.headers << HTTP/1.1 201 Created
    ...
    14657 DEBUG jclouds.wire >> "[{"path":"slo-test/Ni[0xc5][0x9f]an.rar/slo/1491214092.299000/38547913/0/00000001","etag":"58f06dd588d8ffb3beb46ada6309436b","size_bytes":33554432},{"path":"slo-test/Ni[0xc5][0x9f]an.rar/slo/1491214092.299000/38547913/0/00000002","etag":"2b4b81733d0a2e4abe89516639627408","size_bytes":4993481}]"
    14657 DEBUG jclouds.headers >> PUT http://137.205.194.8:8080/v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test/Ni%C5%9Fan.rar?multipart-manifest=put HTTP/1.1
    14657 DEBUG jclouds.headers >> Accept: application/json
    ...
    14657 DEBUG jclouds.headers >> Content-Type: application/unknown
    14657 DEBUG jclouds.headers >> Content-Length: 272
    14657 DEBUG jclouds.headers >> Content-Disposition: Nişan.rar

    14657 ERROR org.jclouds.http.internal.JavaUrlHttpCommandExecutorService error after writing 0/272 bytes to http://137.205.194.8:8080/v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test/Ni%C5%9Fan.rar?multipart-manifest=put
    java.io.IOException: too many bytes written
    	at sun.net.www.protocol.http.HttpURLConnection$StreamingOutputStream.write(HttpURLConnection.java:3505)
    	at com.google.common.io.CountingOutputStream.write(CountingOutputStream.java:53)
    	at com.google.common.io.ByteStreams.copy(ByteStreams.java:179)
    	at org.jclouds.http.internal.JavaUrlHttpCommandExecutorService.writePayloadToConnection(JavaUrlHttpCommandExecutorService.java:298)
    	at org.jclouds.http.internal.JavaUrlHttpCommandExecutorService.convert(JavaUrlHttpCommandExecutorService.java:171)
    	at org.jclouds.http.internal.JavaUrlHttpCommandExecutorService.convert(JavaUrlHttpCommandExecutorService.java:65)
    	at org.jclouds.http.internal.BaseHttpCommandExecutorService.invoke(BaseHttpCommandExecutorService.java:99)
    	at org.jclouds.rest.internal.InvokeHttpMethod.invoke(InvokeHttpMethod.java:90)
    	at org.jclouds.rest.internal.InvokeHttpMethod.apply(InvokeHttpMethod.java:73)
    	at org.jclouds.rest.internal.InvokeHttpMethod.apply(InvokeHttpMethod.java:44)
    	at org.jclouds.reflect.FunctionalReflection$FunctionalInvocationHandler.handleInvocation(FunctionalReflection.java:117)
    	at com.google.common.reflect.AbstractInvocationHandler.invoke(AbstractInvocationHandler.java:87)
    	at com.sun.proxy.$Proxy77.replaceManifest(Unknown Source)
    	at org.jclouds.openstack.swift.v1.blobstore.RegionScopedSwiftBlobStore.completeMultipartUpload(RegionScopedSwiftBlobStore.java:522)
    	at uk.ac.warwick.slo.AbstractJCloudsSLOTest.putSLO(AbstractJCloudsSLOTest.java:70)
    	at uk.ac.warwick.slo.AbstractJCloudsSLOTest.assertCanPutAndGetSLO(AbstractJCloudsSLOTest.java:74)
    	at uk.ac.warwick.slo.AbstractJCloudsSLOTest.sloWithHighByteChars(AbstractJCloudsSLOTest.java:91)
    	at uk.ac.warwick.slo.SwiftJCloudsSLOTest.sloWithHighByteChars(SwiftJCloudsSLOTest.java:7)
    	...

## Behaviour in `python-swiftclient`

This is the reference implementation of a Swift client. The reference client doesn't send a Content-Length header when uploading
the manifest file.

    $ sudo pip install python-swiftclient python-keystoneclient
    ...
    $ fallocate -l 38547913 large.file

    $ swift --debug \
        --os-auth-url http://137.205.194.8:5000/identity/v2.0 \
        --os-tenant-name demo \
        --os-username demo \
        --os-password secretadmin \
        upload --use-slo --segment-size 33554432 \
        --object-name 'Nişan.rar' \
        slo-test large.file
    ...
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test_segments/Ni%C5%9Fan.rar/slo/1490950987.875125/38547913/33554432/00000001 HTTP/1.1" 201 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test_segments/Ni%C5%9Fan.rar/slo/1490950987.875125/38547913/33554432/00000001 -X PUT -H "Content-Length: 4993481" -H "Content-Type: application/swiftclient-segment" -H "X-Auth-Token: gAAAAABY4iFUNJKmTlIJY_U4Zxwwfw3Nqyii8Den-Hpm50-Gk8atNVGTRLIVAy6VSdQYdhF-mMZDjIcCSSgEOi5c3DbkcQWR7CtObvxtay0kcpIFtTn-EVMDZc7aXvWkGVyLmeycjq_yYLGWVhovDY4wb-FmVgJef68MX6DyE-NZgsYegIbWwo0"
    DEBUG:swiftclient:RESP STATUS: 201 Created
    DEBUG:swiftclient:RESP HEADERS: {u'Content-Length': u'0', u'Last-Modified': u'Mon, 03 Apr 2017 10:17:57 GMT', u'Etag': u'2b4b81733d0a2e4abe89516639627408', u'X-Trans-Id': u'txe3ba0482516b4c77aa4bd-0058e22154', u'Date': u'Mon, 03 Apr 2017 10:17:57 GMT', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'txe3ba0482516b4c77aa4bd-0058e22154'}
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test_segments/Ni%C5%9Fan.rar/slo/1490950987.875125/38547913/33554432/00000000 HTTP/1.1" 201 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test_segments/Ni%C5%9Fan.rar/slo/1490950987.875125/38547913/33554432/00000000 -X PUT -H "Content-Length: 33554432" -H "Content-Type: application/swiftclient-segment" -H "X-Auth-Token: gAAAAABY4iFUaZPCUewvcKGGUf7ATU1w1rbI3AvdVRH_hwiquZiudL6kdtxOuoL0yksqqzoD0mErwEszq87el3ZR8kf4dU8k2qV8p01kqyJjIeoHJGQsl984FOvMwRo54FEM-pa7A-Dwik9wKN3gu5qK19aYwT5WLecF0kgRTUDnx9meKEekDic"
    DEBUG:swiftclient:RESP STATUS: 201 Created
    DEBUG:swiftclient:RESP HEADERS: {u'Content-Length': u'0', u'Last-Modified': u'Mon, 03 Apr 2017 10:17:57 GMT', u'Etag': u'58f06dd588d8ffb3beb46ada6309436b', u'X-Trans-Id': u'tx20ac6996e3834d449aee1-0058e22154', u'Date': u'Mon, 03 Apr 2017 10:17:57 GMT', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'tx20ac6996e3834d449aee1-0058e22154'}
    Nişan.rar segment 1
    Nişan.rar segment 0
    DEBUG:requests.packages.urllib3.connectionpool:http://137.205.194.8:8080 "PUT /v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test/Ni%C5%9Fan.rar?multipart-manifest=put HTTP/1.1" 201 0
    DEBUG:swiftclient:REQ: curl -i http://137.205.194.8:8080/v1/AUTH_c7a3e66567ec442080a360d6d23f2dbe/slo-test/Ni%C5%9Fan.rar?multipart-manifest=put -X PUT -H "x-object-meta-mtime: 1490950987.875125" -H "x-static-large-object: true" -H "Content-Type: " -H "X-Auth-Token: gAAAAABY4iFUxunHr2NcDc4OeHop6klCHBujsnPozJC14VxHtyL3NROoQp-h_G3ZsgWMiQ3cXHAv_zr5UpiEjdYd98rJPweceQAOnVgDyEHOF4rId-LNx7kZWOPoXd_j0WWErZaNcXVS6zkaRepZMsTSQ5ZG0MsENFwSMeO9JaXsGT3Cx1qN57M"
    DEBUG:swiftclient:RESP STATUS: 201 Created
    DEBUG:swiftclient:RESP HEADERS: {u'Content-Length': u'0', u'Last-Modified': u'Mon, 03 Apr 2017 10:17:58 GMT', u'Etag': u'"6c8e0a09ec0e0736530b3e4aedbad388"', u'X-Trans-Id': u'tx920634543e1d4abdbc83e-0058e22155', u'Date': u'Mon, 03 Apr 2017 10:17:57 GMT', u'Content-Type': u'text/html; charset=UTF-8', u'X-Openstack-Request-Id': u'tx920634543e1d4abdbc83e-0058e22155'}
    Nişan.rar
