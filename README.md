# CustomLogAnalyzer

## Log Analysis Service (Multi-Stage Test Execution Verification)

This application is a backend service designed to streamline the analysis of logs generated during multi-stage testing, such as pre-patch, post-patch, and agent deployment phases. It accepts four distinct log files and two sets of test cases, performing a granular, high-fidelity verification based on a strict log pattern.

## ✨ Key Features

1. **Multi-Input Support:** Accepts four distinct log files and two distinct sets of test case names.
   - **Log Files:** `base_log`, `before_log`, `after_log`, `post_agent_patch_log`
   - **Test Sets:** `main_json_tests` and `report_json_tests`

2. **Granular Analysis Mapping (Issue 2):** To improve efficiency and ensure correct context, test sets are only run against relevant log files:
   - `main_json_tests` are individually verified against: `base_log`, `before_log`, and `after_log`
   - `report_json_tests` are verified exclusively against: `post_agent_patch_log`

3. **Regex-Based Execution Verification (Issue 3):** Verification is no longer a simple string search. A test case is marked as Found (OK) only if its entry in the log file matches a specific structure indicating the test executed and completed.

## 📋 Log Execution Pattern

The service uses a regular expression to enforce the following structure in the log lines:

```
(Optional Prefix) [LOG_LEVEL] (Optional Text) Test.Case.Name (Optional Separator) Time elapsed: X s (Optional Suffix)
```

**Required Regex Components:**

- **Log Level:** Must be one of `[INFO]`, `[ERROR]`, or `[WARNING]`
- **Test Name:** The exact string provided in the input list
- **Time Elapsed:** Must include the literal phrase `Time elapsed: ` followed by a numeric value (integer or decimal) and the unit `s`, which may or may not be separated by a space (e.g., `1.573 s` or `2s`)

## 🔌 API Endpoint

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/analyze` | Initiates the multi-stage, granular log analysis. |

**Request Body (`AnalysisRequest`) Example:**

```json
{
  "baseLog": "Content of the base log file...",
  "beforeLog": "Content of the before_log file...",
  "afterLog": "Content of the after_log file...",
  "postAgentPatchLog": "Content of the post_agent_patch_log file...",
  "mainJsonTests": "TestA, TestB, TestC",
  "reportJsonTests": "TestD, TestE"
}
```

**Response Body (`AnalysisResult[]`) Structure:**

The output is a consolidated list of results, grouped by log source for clarity in the UI.

| Field | Description |
|-------|-------------|
| `testCaseName` | The name of the test case run. |
| `logSource` | The specific log file analyzed (e.g., `base_log`, `post_agent_patch_log`). |
| `result` | Enum: `Found`, `NotFound`, or `Error`. |
| `summary` | Formatted output: `TestName [LogSource]: STATUS (ResultType)` |
| `message` | Simplified status: `Found`, `Not Found`, or specific error details. |

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
   
   The service will start on its default port (typically **8080**). You can then interact with it via its defined API endpoints, typically sending a JSON body with the request structure shown above.

### Example Service Output Format

When the service endpoint is called, the output summaries will be grouped by log source and adhere to the following format:

```
Base Log Analysis
- org.junit.TestA-Prefix-Text.TestA MethodName [base_log]: OK (Found)
- org.junit.TestB-Missing.TestB MethodName [base_log]: NOT OK (Not Found)

Before Log Analysis
- org.junit.TestA-Prefix-Text.TestA MethodName [before_log]: OK (Found)
- org.junit.TestB-Missing.TestB MethodName [before_log]: NOT OK (Not Found)

After Log Analysis
- org.junit.TestA-Prefix-Text.TestA MethodName [after_log]: OK (Found)
- org.junit.TestB-Missing.TestB MethodName [after_log]: NOT OK (Not Found)

Post Agent Patch Log Analysis
- org.junit.TestC-Prefix-Text.TestC MethodName [post_agent_patch_log]: OK (Found)
- org.junit.TestD-Missing.TestD MethodName [post_agent_patch_log]: NOT OK (Not Found)
```