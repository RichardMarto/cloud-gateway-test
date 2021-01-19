## Build
Command to build application:

	mvn clean install
## Run
Command to run the application:

	java -jar ./target/cloud-gateway-upload-test-0.0.1-SNAPSHOT.jar
## Samples
**OK - Request**
Small text file, with 4 bytes.

    curl --location --request POST 'http://localhost:8080/test' --header 'Content-Type: application/text' --data-binary './cloud-gateway/test.txt'
**OK - Response**
		
	200OK Time:5.24 s Size:38 B
**OK - Logfile**

		2021-01-19 16:05:03.424  INFO 35504 --- [ctor-http-nio-5] br.com.test.UploadFilter : 116
		2021-01-19 16:05:03.424  INFO 35504 --- [ctor-http-nio-5] br.com.test.UploadFilter : 101
		2021-01-19 16:05:03.424  INFO 35504 --- [ctor-http-nio-5] br.com.test.UploadFilter : 115
		2021-01-19 16:05:03.424  INFO 35504 --- [ctor-http-nio-5] br.com.test.UploadFilter : 116
**Error - Request**
Bigger image file, with 1.89 KB.

    curl --location --request POST 'http://localhost:8080/test' --header 'Content-Type: image/jpeg' --data-binary '@/C:/Users/richard_dos_santos/IdeaProjects/cloud-gateway/test.jpg'

**Error - Logfile**

	none
**Error - Response**
			
	none
