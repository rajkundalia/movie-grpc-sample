Build the files from proto for user-service and movie-service.

1. Go inside the module of user-service/movie-service
2. Do a mvn clean compile or do a mvn clean install for the whole project
3. If you're using IDE and do not want to see errors in imports, mark target/generated-sources/protobuf/grpc-java and java as Source Root.

Note: Ideally, the proto files should be kept at a common location.

# Abandoning this since this became way bigger than I expected. There would be work required to setup calling services from API Gateway. 
# Also calling of movie-service from user-service was not thought through.

# However, the configurations are correct, it can help anyone setup gRPC with Spring Boot.

# Commands:

1. mvn clean install
2. Start Eureka Server [because code has client discovery, no use otherwise]
3. Start movie-service
4. Start user-service
5. Install grpcurl https://github.com/fullstorydev/grpcurl?tab=readme-ov-file [one time]
6. Try: [I was trying in Windows Powershell]
   1. grpcurl -plaintext -proto ./src/main/proto/movie_service.proto -import-path ./src/main/proto -d '{\"movie_id\":1}' localhost:9090 movie.MovieService.GetMovie
   2. grpcurl -plaintext -proto ./src/main/proto/movie_service.proto -import-path ./src/main/proto -d '{\"limit\":5, \"genre\":\"Drama\"}' localhost:9090 movie.MovieService.GetTrendingMovies
   3. grpcurl -plaintext -proto ./src/main/proto/user_service.proto -import-path ./src/main/proto -d '{\"user_id\":1}' localhost:9092 user.UserService.GetUserProfile
   
You can generate grpcurl commands using an LLM tool.