@Grab(group='org.apache.httpcomponents.client5', module='httpclient5', version='5.2')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.HttpClients
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.io.entity.mime.MultipartEntityBuilder
import org.apache.hc.core5.http.ContentType
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Files

/**
 * Upload a file as if using OpenAI's /files endpoint.
 */
def uploadFile(String hostname, String authToken, String subscriptionKey, File pdfFile) {
    def client = HttpClients.createDefault()
    def post = new HttpPost("${hostname}/csp/chat-service/api/v1/files")

    def builder = MultipartEntityBuilder.create()
    builder.addBinaryBody("file", pdfFile, ContentType.APPLICATION_PDF, pdfFile.name)
    builder.addTextBody("purpose", "assistants")

    post.setEntity(builder.build())

    post.setHeader("Authorization", "Bearer ${authToken}")
    post.setHeader("Subscription-Key", subscriptionKey)

    def response = client.execute(post)
    def body = response.entity.content.text
    if (response.code >= 200 && response.code < 300) {
        def parsed = new JsonSlurper().parseText(body)
        return parsed.id  // assume this API mirrors OpenAI and returns { id: "file-xxx" }
    } else {
        println "File upload failed: $body"
        return null
    }
}

/**
 * Fallback: upload PDF using multipart directly if /files not supported.
 */
def uploadPdfMultipart(String hostname, String authToken, String subscriptionKey, File pdfFile) {
    def client = HttpClients.createDefault()
    def post = new HttpPost("${hostname}/csp/chat-service/api/v1/chat/completions")

    def builder = MultipartEntityBuilder.create()
    builder.addBinaryBody("file", pdfFile, ContentType.APPLICATION_PDF, pdfFile.name)
    builder.addTextBody("model", "GPT_35_TURBO")
    builder.addTextBody("prompt", "Please analyze this PDF file.")

    post.setEntity(builder.build())

    post.setHeader("Authorization", "Bearer ${authToken}")
    post.setHeader("Subscription-Key", subscriptionKey)

    def response = client.execute(post)
    def body = response.entity.content.text
    return body
}

/**
 * Create a chat completion request, referencing the uploaded file.
 */
def createChatCompletion(String hostname, String authToken, String subscriptionKey, String fileId) {
    def client = HttpClients.createDefault()
    def post = new HttpPost("${hostname}/csp/chat-service/api/v1/chat/completions")

    def body = [
        model: "GPT_35_TURBO",
        messages: [
            [ role: "user", content: "Summarize the PDF I uploaded." ]
        ],
        file_ids: [ fileId ]  // assumes API mirrors OpenAI Assistants conventions
    ]

    def jsonBody = JsonOutput.toJson(body)
    post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON))

    post.setHeader("Authorization", "Bearer ${authToken}")
    post.setHeader("Subscription-Key", subscriptionKey)
    post.setHeader("Content-Type", "application/json")

    def response = client.execute(post)
    def responseBody = response.entity.content.text

    def parsed = new JsonSlurper().parseText(responseBody)
    return parsed?.choices?.getAt(0)?.message?.content
}

// =======================
// Usage Example
// =======================

def hostname = System.getenv("API_HOST") ?: "https://somewhere.someplace.com"
def authToken = System.getenv("AUTH_TOKEN") ?: "<INJECT_TOKEN>"
def subscriptionKey = System.getenv("SUBSCRIPTION_KEY") ?: "<INJECT_KEY>"
def pdfFile = new File("/path/to/my.pdf")

// Try "files" style upload first
def fileId = uploadFile(hostname, authToken, subscriptionKey, pdfFile)

if (fileId) {
    println "File uploaded successfully: ${fileId}"
    def answer = createChatCompletion(hostname, authToken, subscriptionKey, fileId)
    println "LLM Response: $answer"
} else {
    println "Falling back to direct multipart PDF upload..."
    def answer = uploadPdfMultipart(hostname, authToken, subscriptionKey, pdfFile)
    println "LLM Response: $answer"
}
