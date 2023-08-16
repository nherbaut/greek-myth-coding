clean:
	mvn clean
build:
	mvn package -DskipTests=true
run:	
	java -jar target/greek-myth-coding-0.0.1-SNAPSHOT.jar
debug: 
	java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 target/greek-myth-coding-0.0.1-SNAPSHOT.jar

debugs: 
	java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 target/greek-myth-coding-0.0.1-SNAPSHOT.jar

