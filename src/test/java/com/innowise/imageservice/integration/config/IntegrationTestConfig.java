package com.innowise.imageservice.integration.config;

import com.innowise.imageservice.integration.config.annotation.IntegrationTests;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@IntegrationTests
@Sql(scripts = {
        "classpath:db/delete-data.sql"}, executionPhase = BEFORE_TEST_METHOD)
public abstract class IntegrationTestConfig {

    static final String BUCKET_NAME = "images";

    @Container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    protected static final LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.3"))
                    .withServices(S3);

    protected S3Client s3Client;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("app.bucket", ()->BUCKET_NAME);
        registry.add("aws.s3.endpoint-url", () -> localstack.getEndpointOverride(S3).toString());
        registry.add("aws.access-key", localstack::getAccessKey);
        registry.add("aws.secret-key", localstack::getSecretKey);
        registry.add("aws.region", localstack::getRegion);
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localstack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
    }

    @BeforeEach
    void setupS3Client() {
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(S3))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        localstack.getAccessKey(),
                                        localstack.getSecretKey()
                                )
                        )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

        s3Client.listBuckets().buckets().forEach(bucket -> {
            String bucketName = bucket.name();
            s3Client.listObjectsV2(builder -> builder.bucket(bucketName).build())
                    .contents()
                    .forEach(object ->
                            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(object.key()).build())
                    );
        });
    }
}
