
# RabbitMQ Practices

This project demonstrates the integration of a Spring Boot application with RabbitMQ for message publishing and consuming. It includes examples of producer, consumer, and integration tests using Testcontainers.


---

## Technologies Used

- **Java**: 21
- **Spring Boot**: 3.4.4
- **RabbitMQ**: Message broker
- **Maven**: Build tool
- **Testcontainers**: For integration testing with RabbitMQ
- **Mockito**: For mocking in unit tests

---

## Setup and Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/rabbitMQPractices.git
   cd rabbitMQPractices
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run RabbitMQ locally or use Docker:
   ```bash
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
   ```

4. Start the application:
   ```bash
   mvn spring-boot:run
   ```

---

## Project Structure

```
src
├── main
│   ├── java/com/example/rabbitMQPractices
│   │   ├── controller/BlogPostController.java  # REST API for blog posts
│   │   ├── entity/BlogPost.java               # BlogPost entity
│   │   ├── producer/BlogPostProducer.java     # RabbitMQ producer
│   ├── resources
│       ├── application.properties             # Application configuration
├── test
│   ├── java/com/example/rabbitMQPractices
│   │   ├── integration/RabbitMQIntegrationTest.java  # Integration tests
│   │   ├── RabbitMqPracticesApplicationTests.java    # Context load test
```

---

## Features

- **Producer**: Sends blog post messages to RabbitMQ.
- **Consumer**: Listens to messages from RabbitMQ queues.
- **Integration Tests**: Verifies RabbitMQ integration using Testcontainers.
- **Error Handling**: Handles invalid messages gracefully.

---

## Running the Application

1. Ensure RabbitMQ is running locally or via Docker.
2. Start the application:
   ```bash
   mvn spring-boot:run
   ```
3. Access the RabbitMQ management UI at `http://localhost:15672` (default credentials: `guest/guest`).

---

## Testing

Run the tests using Maven:
```bash
mvn test
```

### Key Tests:
- **Integration Tests**: Located in `src/test/java/com/example/rabbitMQPractices/integration/RabbitMQIntegrationTest.java`.
- **Context Load Test**: Ensures the Spring Boot application context loads correctly.

---

## API Endpoints

### Create Blog Post
**POST** `/api/blog-posts`

**Request Body**:
```json
{
  "title": "Sample Title",
  "content": "Sample Content"
}
```

**Response**:
```
Blog post sent: BlogPost{id='generated-uuid', title='Sample Title', content='Sample Content'}
```

---

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
```