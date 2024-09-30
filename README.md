# Agniv Setup Guide

This guide will help you set up and run our Spring Boot application. Please follow these steps carefully to ensure a smooth setup process.

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Maven
- PostgreSQL Database
- Ollama (for AI functionality)
- PgVectorDB

## Configuration

The application uses the following configuration. Make sure to adjust these settings in your `application.properties` file:

```properties
spring.application.name=agnivbackend
spring.datasource.driver=cdata.jdbc.postgresql.PostgreSQLDriver
spring.datasource.password=YOUR_PASSWORD
spring.datasource.username=YOUR_USERNAME
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/agnivdata
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.ai.ollama.chat.model=tinyllama
```

### Important Notes:

1. The application name is set to "agnivbackend".
2. We're using a PostgreSQL driver (`cdata.jdbc.postgresql.PostgreSQLDriver`) and database.
3. The database is expected to be running on localhost (127.0.0.1) on port 5432 (standard PostgreSQL port).
4. The database name is "agnivdata".
5. The application will automatically update the database schema (`spring.jpa.hibernate.ddl-auto=update`).
6. SQL queries will be shown in the console (`spring.jpa.show-sql=true`).
7. The application uses Ollama AI with the "tinyllama" model.

## Setup Steps

1. **Database Setup**:
    - Install PostgreSQL if not already installed.
    - Create a database named `agnivdata`.
    - Make sure the PostgreSQL server is running on localhost:5432.

2. **Application Properties**:
    - Review the `application.properties` file and adjust any settings if necessary.
    - Ensure the database username and password are correct.

3. **Ollama Setup**:
    - Install Ollama on your system.
    - Make sure the "tinyllama" model is available for Ollama.

4. **Build the Application**:
   ```
   mvn clean install
   ```

5. **Run the Application**:
   ```
   mvn spring-boot:run
   ```

## Troubleshooting

- If you encounter database connection issues, double-check your PostgreSQL installation and the connection details in `application.properties`.
- For AI-related problems, ensure Ollama is properly installed and the "tinyllama" model is available.
- If you see any "Bean not found" errors, make sure all required dependencies are included in your `pom.xml` file.

## Additional Information

- The application uses JPA for database operations.
- Hibernate is configured to automatically update the database schema.
- SQL queries will be logged to the console for debugging purposes.
- The application is using a custom PostgreSQL driver (`cdata.jdbc.postgresql.PostgreSQLDriver`). Ensure this driver is properly included in your project dependencies.

For any further assistance, please contact the development team or raise an issue in the project repository.
