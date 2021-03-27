## WmRndInvokeInterceptor

To know more about this project please visit [here](https://raw.githubusercontent.com/johnpcarter/WmRndInvokeInterceptor)


# Setup

- Copy the file invokemanager.cnf to <SAG_HOME>/IntegrationServer/instances/default/config directory.
- Create the jar file via exportJarToWm107TestPackage jar exporter, update the path to either the static directory of one of packages code/jars directory or save it to the IntegrationServer/lib/jars directory.
- You will also need to copy the jar file ./lib/org.apache.comon.jxpath_1.3.0.jar to the same target directory above.
- Restart your server.

# Usage

- Create a webMethods package with a service that then invokes several sub services. Change the audit level of the services to "always" and the level to "start, success and error".
- Go to the logged fields tab and select some fields to log as well.
- Update the chainPreProcessor() and chainPostProcessor() to call your code.
- Use the following curl command to invoke your service, editing the url path to match your service.

`curl "http://localhost:5555/invoke/jc.test:hello?name=bob"
     -H 'etoe-transaction-id: f10f14ac-8297-11eb-8dcd-0242ac130003' 
     -u 'Administrator:manage'` 
 

