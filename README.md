# CustomLogAnalyzer

A robust Java and Spring Boot application designed to analyze log content against a list of predefined test names, providing a clear and concise pass/fail report.

## 🚀 Project Description

The **CustomLogAnalyzer** is a utility that processes two primary inputs: a list of test names and the complete log content.

Its core function is to systematically search for each test name within the log file. The service supports **case-insensitive matching** for high reliability.

The output is a simplified, clear report detailing the status of each test: **OK (Found)** or **NOT OK (Not Found)**. This clean format facilitates rapid analysis and integration into larger systems.

## ✨ Key Features

- **Concise Reporting:** Generates clean output in the format `TestName: STATUS (Found/Not Found)`.
- **Case-Insensitive Search:** Ensures reliable matching regardless of the case used in the log file or the test name.
- **Flexible Input Parsing:** Handles test lists separated by newlines, commas, or semicolons.
- **Robust Error Handling:** Reports system-level issues (like missing input) gracefully as `SystemCheck: ERROR`.

## 💻 Prerequisites

To set up, build, and run this project, you need the following installed:

- **Java Development Kit (JDK) 17 or later**
- **Apache Maven 3.6.0 or later**

## 📦 Setup and Build Instructions

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/YourUsername/CustomLogAnalyzer.git
   cd CustomLogAnalyzer
   ```

2. **Verify Prerequisites:**
   
   Ensure Java and Maven are correctly configured in your environment:
   ```bash
   java -version
   mvn -version
   ```

3. **Build the Project:**
   
   Use Maven to compile the source code, run unit tests, and package the application:
   ```bash
   mvn clean install
   ```
   
   This command compiles the code, runs all tests, and packages the application into an executable JAR file in the `target/` directory.

## ▶️ Run Instructions (Spring Boot Service)

Since this is a Spring Boot service, the recommended way to run it is via the Spring Boot Maven plugin.

1. **Execute the Application:**
   ```bash
   mvn spring-boot:run
   ```
   
   The service will start on its default port (typically **8080**). You can then interact with it via its defined API endpoints, typically sending a JSON body with the `testCaseListInput` and `logContentInput`.

### Example Service Output Format

When the service endpoint is called, the output summaries will adhere to the simplified format:

```
--- Analysis Summary ---
TestA: OK (Found)
TestB: OK (Found)
TestC: NOT OK (Not Found)
```