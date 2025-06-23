import com.jcraft.jsch.*
import java.nio.file.*
import java.util.Properties
import org.apache.log4j.Logger // Import Log4j Logger

// Add the BaseScript annotation to make the 'log' variable available
@groovy.transform.BaseScript com.ser.blueline.groovy.CSBScript HL7FileProcessorScript

class HL7FileProcessor {
    private Logger logger // Change from 'log' to 'logger' to avoid conflict and clearly define it
    private String sourceDirectory
    private String stagingDirectory
    private String sftpDestination
    private String sftpUsername
    private String sftpPassword
    private String sftpDestinationDir
    private String eventDocumentId

    // Counters for summary
    private int filesFoundCount = 0
    private int filesMovedToStagingCount = 0
    private int filesUploadedCount = 0
    private int filesDeletedCount = 0

    HL7FileProcessor(Logger agentLogger, String uidlys_sftpDestination, String uidlys_sftpDestinationDir, String uidlys_sftpUsername, String uidlys_sftpPassword, String uidlys_sourceDirectory, String eventDocIdParam) {
        this.logger = agentLogger // Assign the passed logger instance
        logger.info "Initializing HL7FileProcessor. V2.1"

        // Get environment variables
        this.sourceDirectory = uidlys_sourceDirectory
        logger.info "Source directory is: ${sourceDirectory}"

        this.sftpDestination = uidlys_sftpDestination
        logger.info "SFTP destination is: ${sftpDestination}"

        this.sftpUsername = uidlys_sftpUsername
        logger.info "SFTP username is: ${sftpUsername}"
        
        this.sftpDestinationDir = uidlys_sftpDestinationDir
        logger.info "SFTP destination directory is: ${sftpDestinationDir}"

        this.sftpPassword = uidlys_sftpPassword
        // Note: Logging the password directly is generally not recommended for security reasons.
        logger.info "SFTP password retrieved (not logged in info level)"

        // Assign the event document ID passed from the script's main execution context
        this.eventDocumentId = eventDocIdParam
        logger.info "Event Document ID received: ${this.eventDocumentId}"

        // Validate environment variables
        logger.info "Validating required environment variables..."
        if (!this.sourceDirectory || !this.sftpDestination || !this.sftpUsername || !this.sftpPassword || !this.sftpDestinationDir) {
          logger.error "ERROR: Required environment variables are not set. sourceDir: ${sourceDirectory}, sftpDest: ${sftpDestination}, sftpUser: ${sftpUsername}, sftpUserPass, sftpDestDir: ${sftpDestinationDir}"  
          throw new IllegalStateException("Required environment variables are not set")
        }
        logger.info "All required environment variables are set."

        // Create staging directory if it doesn't exist
        File stagingDir = new File(this.sourceDirectory, "Staging")
        stagingDirectory = stagingDir.getAbsolutePath()
        if (!stagingDir.exists()) {
            logger.info "Staging directory '${stagingDirectory}' does not exist. Creating it..."
            stagingDir.mkdirs()
            logger.info "Staging directory '${stagingDirectory}' created successfully."
        } else {
            logger.info "Staging directory '${stagingDirectory}' already exists."
        }
    }

    void processFiles() {
        logger.info "Starting the processFiles method."
        try {
            // Get all HL7 files from source directory
            logger.info "Getting HL7 files from source directory: ${sourceDirectory}"
            def sourceDir = new File(sourceDirectory)
            def hl7Files = sourceDir.listFiles().findAll {
                def isHL7 = it.name.toLowerCase().endsWith('.hl7')
                logger.info "File '${it.name}' ends with '.hl7': ${isHL7}" // Changed to debug as it's internal check
                return isHL7
            }
            this.filesFoundCount = hl7Files.size() // Update counter
            logger.info "Found ${this.filesFoundCount} HL7 files in source directory."

            // Move files to staging
            logger.info "Moving files to staging directory: ${stagingDirectory}"
            hl7Files.each { file ->
                if (moveToStaging(file)) { // Modify moveToStaging to return boolean
                    this.filesMovedToStagingCount++ // Update counter
                }
            }
            logger.info "Finished moving ${this.filesMovedToStagingCount} files to staging."

            // Process files in staging
            def stagingDir = new File(stagingDirectory)
            def stagedFiles = stagingDir.listFiles().findAll {
                def isHL7 = it.name.toLowerCase().endsWith('.hl7')
                logger.info "File '${it.name}' in staging ends with '.hl7': ${isHL7}" // Changed to debug
                return isHL7
            }
            // Note: stagedFiles.size() might be different from filesMovedToStagingCount if files were already in staging
            logger.info "Found ${stagedFiles.size()} HL7 files in staging directory (including potentially pre-existing ones)."

            // Upload to SFTP and delete staged files
            logger.info "Processing files in staging: uploading to SFTP and deleting."
            stagedFiles.each { file ->
                logger.info "Processing staged file: ${file.name}"
                if (uploadToSftp(file)) {
                    this.filesUploadedCount++ // Update counter
                    if (deleteFile(file)) { // Modify deleteFile to return boolean
                        this.filesDeletedCount++ // Update counter
                    }
                }
            }
            logger.info "Finished processing files in staging."

        } catch (Exception e) {
            logger.error "Error processing files: ${e.message}"
            e.printStackTrace() // Keep printStackTrace for detailed error
        } finally {
            logger.info "Finished the processFiles method."
        }
    }

    // Changed return type to boolean to indicate success
    private boolean moveToStaging(File file) {
        logger.info "Moving file '${file.name}' to staging directory."
        try {
            def destination = new File(stagingDirectory, file.name)
            logger.info "Moving '${file.absolutePath}' to '${destination.absolutePath}'" // Changed to debug
            Files.move(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            logger.info "Moved file '${file.name}' to staging directory."
            return true
        } catch (Exception e) {
            logger.error "Error moving file '${file.name}' to staging: ${e.message}"
            e.printStackTrace()
            return false
        }
    }

    private boolean uploadToSftp(File file) {
        logger.info "Starting SFTP upload for file: ${file.name}"
        JSch jsch = new JSch()
        Session session = null
        ChannelSftp channelSftp = null

        try {
            logger.info "Creating SFTP session to ${sftpDestination} with username: ${sftpUsername}"
            session = jsch.getSession(sftpUsername, sftpDestination)
            session.setPassword(sftpPassword)
            logger.info "SFTP password set for session." // Changed to debug

            Properties config = new Properties()
            config.put("StrictHostKeyChecking", "no")
            session.setConfig(config)
            logger.info "Set StrictHostKeyChecking to 'no'." // Changed to debug

            logger.info "Connecting to SFTP server..."
            session.connect()
            logger.info "Successfully connected to SFTP server."

            logger.info "Opening SFTP channel."
            channelSftp = (ChannelSftp) session.openChannel("sftp")
            channelSftp.connect()
            logger.info "SFTP channel connected."

            def remotePath = "${sftpDestinationDir}/${file.name}"
            logger.info "Uploading file '${file.absolutePath}' to remote path: '${remotePath}'"

            // Read file content and log it with trace level, including the event document ID
            try {
                String fileContent = new String(Files.readAllBytes(file.toPath()))
                // Ensure eventDocumentId is not null or empty before logging
                String idToLog = (eventDocumentId != null && !eventDocumentId.isEmpty()) ? eventDocumentId : "N/A - Invalid Event Document ID"
                logger.info "Event Document ID: ${idToLog}, Contents of file '${file.name}':\n${fileContent}" //changed to debug
            } catch (IOException e) {
                logger.warn "Could not read content of file '${file.name}' for logging: ${e.message}"
            }

            channelSftp.put(file.absolutePath, remotePath)
            logger.info "Successfully uploaded '${file.name}' to SFTP."
            return true

        } catch (Exception e) {
            logger.error "Error uploading file '${file.name}' to SFTP: ${e.message}"
            e.printStackTrace()
            return false
        } finally {
            logger.info "Closing SFTP resources for file: ${file.name}"
            if (channelSftp != null) {
                logger.info "Disconnecting SFTP channel." // Changed to debug
                channelSftp.disconnect()
                logger.info "SFTP channel disconnected." // Changed to debug
            }
            if (session != null) {
                logger.info "Disconnecting SFTP session." // Changed to debug
                session.disconnect()
                logger.info "SFTP session disconnected." // Changed to debug
            }
            logger.info "SFTP resources closed for file: ${file.name}"
        }
    }

    // Changed return type to boolean to indicate success
    private boolean deleteFile(File file) {
        logger.info "Deleting file '${file.name}' from staging directory."
        try {
            logger.info "Deleting file at path: '${file.toPath()}'" // Changed to debug
            Files.delete(file.toPath())
            logger.info "Deleted file '${file.name}' from staging directory."
            return true
        } catch (Exception e) {
            logger.error "Error deleting file '${file.name}': ${e.message}"
            e.printStackTrace()
            return false
        }
    }
}

// Main execution
log.info "Starting HL7FileProcessor execution."
long startTime = System.currentTimeMillis() // Record start time

// Get the ID of the event document, if available.
// The 'eventDocument' variable is provided by the CSBScript base class.
// It might be null if the script is not triggered by an event involving a Doxis4 document.
String eventDocId = "N/A - No Event Document" // Default value
if (eventDocument != null) {
    String retrievedId = eventDocument.getID()
    if (retrievedId != null && !retrievedId.isEmpty()) {
        eventDocId = retrievedId
    } else {
        eventDocId = "N/A - Event Document ID is null or empty"
    }
}
// Pass the 'log' object from the base script to the HL7FileProcessor constructor
def processor = new HL7FileProcessor(log, SFTPHOST, SFTPDESTDIR, SFTPUSER, SFTPPASS, SOURCEDIR, eventDocId)
processor.processFiles()

long endTime = System.currentTimeMillis() // Record end time
long durationMillis = endTime - startTime
String durationString = String.format("%.2f seconds", durationMillis / 1000.0)

// Construct the summary message
String summaryMessage = "HL7 File Processing completed: " +
                        "Found ${processor.filesFoundCount} HL7 files. " +
                        "Moved ${processor.filesMovedToStagingCount} to staging. " +
                        "Uploaded ${processor.filesUploadedCount} to SFTP. " +
                        "Deleted ${processor.filesDeletedCount} from staging. " +
                        "Job run time: ${durationString}."

log.info "Completed HL7FileProcessor execution. Job run time: ${durationString}."
// Return the summary message as the agent's execution message
resultSuccess(summaryMessage)

