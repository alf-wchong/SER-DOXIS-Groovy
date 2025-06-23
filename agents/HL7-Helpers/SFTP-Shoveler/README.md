# HL7 message file Send to SFTP Groovy Script

This Groovy script is designed to process HL7 (Health Level Seven) files from a specified source directory, move them to a staging area, upload them to a remote SFTP server, and then delete them from the staging directory upon successful transfer. It is intended to run within a Doxis4 Agent Server, leveraging the `GroovyBlueline` library for Doxis4-specific functionalities, particularly for logging and event handling.

## Features

* **Automated File Processing**: Monitors a source directory for `.hl7` files.

* **Staging Area**: Files are moved to a temporary "Staging" directory before SFTP upload, ensuring atomicity and preventing partial transfers of source files.

* **SFTP Integration**: Securely uploads processed HL7 files to a configurable SFTP server.

* **Post-Processing Cleanup**: Deletes files from the staging directory only after successful SFTP upload.

* **Robust Logging**: Utilizes the `Log4j` logger provided by `GroovyBlueline` to record detailed operational steps, debug information, warnings, and errors.

  * `log.info`: General process flow information.

  * `log.warn`: Warnings for non-critical issues (e.g., unable to read file content for trace logging).

  * `log.error`: Critical errors during file processing or SFTP transfer.

* **Event Document ID Tracking**: Logs the ID of the Doxis4 document that potentially triggered the script, providing crucial context for event-driven agents.

* **Execution Summary**: Provides a concise summary of the agent's activity (files found, moved, uploaded, deleted, and total run time) as the job completion message.

* **Error Handling**: Includes `try-catch` blocks for robust error management, printing stack traces for detailed troubleshooting.

## Prerequisites

To run this script, you will need:

* **Doxis4 Agent Server: The script is designed to run within the Doxis4 ecosystem.

* **GroovyBlueline Library**: Specifically `GroovyBlueline80p1.jar` or a compatible version, which provides the `CSBScript` base class.

* **JSch Library**: The `com.jcraft.jsch` library (a pure Java SSH2 client) is required for SFTP connectivity. This JAR file (`jsch.jar`) must be available on the classpath of your Groovy environment (e.g., in `agents/lib/custom` for Agent Server or `csbcli/lib/custom` for CSBCmd).

* **Configured Environment Variables**: The script relies on several environment variables (see table below) for SFTP connection details and file paths:

  * `SFTPHOST`: The SFTP server hostname or IP address.

  * `SFTPDESTDIR`: The destination directory on the SFTP server.

  * `SFTPUSER`: The SFTP username.

  * `SFTPPASS`: The SFTP password.

  * `SOURCEDIR`: The local directory where HL7 files are initially located.

## Setup

1. Create a new agent definition in the Doxis4 Admin Client.
   
2. **Deploy JSch**: Import `jsch.jar` file using the upload library function for the new agent.

3. **Configure Environment Variables**: Set the five required environment variables (`SFTPHOST`, `SFTPDESTDIR`, `SFTPUSER`, `SFTPPASS`, `SOURCEDIR`) as the new agent's parameters (see table below).

4. **Place the Groovy Script**: Save the provided Groovy code into the script editor for the new agent.

## Usage

This script is executed:

1. **As a Doxis4 Agent**: Configure an Agent in your Doxis4 system to trigger this script based on specific events (e.g., a new document is archived, a workflow step is completed). The `eventDocument` variable will automatically be populated if an event document triggers the agent.


This script requires the following agent parameters

| Row | ID | Qualified name      | Short name      | Description                                     | Example Value                    |
|-----|----|---------------------|-----------------|-------------------------------------------------|----------------------------------|
| 1   | 🔍 | uidlys_sftpPass     | SFTPPASS        | The password for connecting to the SFTP server. | `your-sftp-password`             |
| 2   | 🔍 | uidlys_sourceDir    | SOURCEDIR       | The local directory to monitor for HL7 files.   | `/home/doxis4/workingdir/`       |
| 3   | 🔍 | uidlys_sftpUser     | SFTPUSER        | The username for connecting to the SFTP server. | `sftpuser`                       |
| 4   | 🔍 | uidlys_sftpHost     | SFTPHOST        | The hostname or IP address of the SFTP server.  | `sftp.example.com`               |
| 5   | 🔍 | uidlys_sftpDestDir  | SFTPDESTDIR     | The destination directory on the SFTP server.   | `/uploads/hl7/`                  |
