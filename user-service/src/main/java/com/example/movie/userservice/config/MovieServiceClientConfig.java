package com.example.movie.userservice.config;

import com.example.movie.movieservice.proto.MovieServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class MovieServiceClientConfig {

    private final DiscoveryClient discoveryClient;

    private ManagedChannel channel;

    public MovieServiceClientConfig(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Bean
    public MovieServiceGrpc.MovieServiceBlockingStub movieServiceBlockingStub() {
        channel = createChannel();
        return MovieServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    public MovieServiceGrpc.MovieServiceStub movieServiceStub() {
        if (channel == null || channel.isShutdown()) {
            channel = createChannel();
        }
        return MovieServiceGrpc.newStub(channel);
    }

    private ManagedChannel createChannel() {
        // Try to get movie-service from Eureka
        List<ServiceInstance> instances = discoveryClient.getInstances("movie-service");

        if (instances.isEmpty()) {
            log.warn("No instances of movie-service found in Eureka. Using default configuration.");
            // Default configuration if service discovery fails
            return ManagedChannelBuilder.forAddress("localhost", 9090)
                    .usePlaintext()
                    .build();
        }

        // Get the first instance
        ServiceInstance serviceInstance = instances.get(0);

        // Check if the gRPC port is in metadata
        String grpcPort = serviceInstance.getMetadata().get("gRPC.port");
        int port = grpcPort != null ? Integer.parseInt(grpcPort) : 9090; // Default to 9090 if not specified

        log.info("Creating gRPC channel to movie-service at {}:{}", serviceInstance.getHost(), port);

        return ManagedChannelBuilder.forAddress(serviceInstance.getHost(), port)
                .usePlaintext()
                .build();
    }

    @PreDestroy
    public void close() {
        if (channel != null) {
            log.info("Shutting down gRPC channel to movie-service");
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("Error shutting down gRPC channel: {}", e.getMessage());
            }
        }
    }
}