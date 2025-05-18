Build the files from proto for user-service and movie-service.

1. Go inside the module of user-service/movie-service
2. Do a mvn clean compile or do a mvn clean install for the whole project
3. If you're using IDE and do not want to see errors in imports, mark target/generated-sources/protobuf/grpc-java and java as Source Root.

Note: Ideally, the proto files should be kept at a common location.

# Abandoning this since this became way bigger than I expected. There would be work required to setup calling services from API Gateway. 
# Also calling of movie-service from user-service was not thought through.

# However, the configurations are correct, it can help anyone setup gRPC with Spring Boot.
