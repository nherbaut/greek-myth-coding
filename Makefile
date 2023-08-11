all:
	mvn package -DskipTests=true
	java -jar target/greek-myth-coding-0.0.1-SNAPSHOT.jar
