package org.cyoda.example.webserver.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.hex
import kotlinx.html.*
import kotlinx.serialization.Serializable
import org.cyoda.example.hello.serializations.UuidKSerializer
import org.cyoda.example.webserver.conf.loadCookieConfig
import org.cyoda.example.webserver.conf.loadClientConnectionProperties
import org.cyoda.example.webserver.connect.loginRoute
import org.cyoda.example.webserver.connect.logoutRoute
import org.cyoda.example.webserver.connect.standardClient
import org.cyoda.example.webserver.entitymodel.EntityModelDto
import org.cyoda.example.webserver.entitymodel.deleteModelRoute
import org.cyoda.example.webserver.entitymodel.fetchEntityModels
import org.cyoda.example.webserver.entitymodel.lockUnlockModelRoute
import org.cyoda.example.webserver.entitymodel.registerModelRoute
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

// Session class to store the access token
@Serializable
data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val expiryTime: Long,
    val refreshUrl: String
) : Principal

// Login Request and Response data classes
@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(
    @Serializable(with = UuidKSerializer::class) val userId: UUID,
    val token: String,
    val refreshToken: String,
    val username: String
)

fun generateSecretKeySpec(algorithm: String, keySize: Int): SecretKeySpec {
    val keyGenerator = KeyGenerator.getInstance(algorithm)
    keyGenerator.init(keySize * 8) // Initialize with key size in bits (16 bytes = 128 bits)
    val secretKey: SecretKey = keyGenerator.generateKey()
    return SecretKeySpec(secretKey.encoded, algorithm)
}

fun Application.configureRouting() {
    // Load configuration
    val apiConfig = loadCookieConfig()
    val connProps = loadClientConnectionProperties()

    // Install necessary Ktor features
    install(Sessions) {
        // If missing, generate AES key for encryption. AES key must be 16, 24, or 32 bytes
        val secretEncryptKey = apiConfig.cookieEncryptKey
            ?.let { SecretKeySpec(hex(it),"AES") }
            ?: generateSecretKeySpec("AES", 16)

        // If missing, generate HmacSHA256 key for signing. Recommended size for HmacSHA256 is 32 bytes
        val secretSignKey = apiConfig.cookieSignKey
            ?.let { SecretKeySpec(hex(it),"HmacSHA256") }
            ?: generateSecretKeySpec("HmacSHA256", 32)

        cookie<UserSession>("user_session", SessionStorageMemory()) {
            cookie.path = "/"
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
        }
    }

    val client = standardClient()

    routing {
        // Landing Page with Login and Entity Registration
        get("/") {
            val session = call.sessions.get<UserSession>()

            call.respondHtml {
                head {
                    title { +"Entity Model Management" }
                    link(rel = "stylesheet", href = "/static/styles.css", type = "text/css")
                }
                body {
                    div(classes = "container") {

                        if (session == null) {
                            h2 { +"Login to access the system" }
                            loginForm()
                        } else {
                            hr()
                            h2 { +"Entity Models" }

                            // Display scrollable table of entity models
                            div {
                                style = "overflow-x: auto; text-align: center; padding: 10px; margin-bottom: 20px;" // Scrollable container for the table
                                try {
                                    val entityModels = fetchEntityModels(client, connProps, session)
                                    if (entityModels.isNotEmpty()) {
                                        table(classes = "entity-models-table") {
                                            // Table headers
                                            thead {
                                                tr {
                                                    th { +"Name" }
                                                    th { +"Version" }
                                                    th { +"State" }
                                                    th { +"Action" }
                                                    th { +"Delete" }
                                                }
                                            }

                                            // Table rows
                                            tbody {
                                                entityModels.forEach { model ->
                                                    tr {
                                                        td { +model.modelName }
                                                        td { +model.modelVersion.toString() }
                                                        td { +model.currentState }
                                                        td { controlModelForm(model) }
                                                        td { deleteModelForm(model) }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        p { +"No entity models available." }
                                    }
                                } catch (e: Exception) {
                                    p { +"Failed to load entity models: ${e.message}" }
                                }
                            }

                            hr()
                            h2 { +"Register Entity Model" }
                            registerEntityModelForm()

                            hr()
                            logoutButton()
                        }
                    }
                }
            }
        }

        // Cyoda Routes
        loginRoute(client, connProps)
        logoutRoute()
        registerModelRoute(client, connProps)
        lockUnlockModelRoute(client, connProps)
        deleteModelRoute(client, connProps)
        staticResources("/static", "static")
    }
}

fun DIV.loginForm() {
    form(action = "/login", method = FormMethod.post, classes = "login-form") {
        textInput(name = "username") {
            placeholder = "Username"
        }
        br()
        passwordInput(name = "password") {
            placeholder = "Password"
        }
        br()
        submitInput { value = "Login" }
    }
}

fun DIV.logoutButton() {
    form(action = "/logout", method = FormMethod.post) {
        submitInput { value = "Logout" }
    }
}


fun TD.controlModelForm(model: EntityModelDto) {
    form(action = "/lock-unlock-model", method = FormMethod.post) {
        input(type = InputType.hidden, name = "modelName") { value = model.modelName }
        input(type = InputType.hidden, name = "modelVersion") { value = model.modelVersion.toString() }

        if (model.currentState == "LOCKED") {
            input(type = InputType.radio, name = "action") {
                value = "UNLOCK"
                onClick = "this.form.submit();" // Automatically submit on click
            }
            label { +"Unlock" }
        } else {
            input(type = InputType.radio, name = "action") {
                value = "LOCK"
                onClick = "this.form.submit();" // Automatically submit on click
            }
            label { +"Lock" }
        }
    }
}


fun TD.deleteModelForm(model: EntityModelDto) {
    form(action = "/delete-model", method = FormMethod.post) {
        input(type = InputType.hidden, name = "modelName") { value = model.modelName }
        input(type = InputType.hidden, name = "modelVersion") { value = model.modelVersion.toString() }

        input(type = InputType.submit) {
            value = "Delete"
            // JavaScript confirmation before submitting the delete form
            onClick =
                "return confirm('Are you sure you want to delete the model named \"${model.modelName}\" (version ${model.modelVersion})?');"
        }
    }
}


fun DIV.registerEntityModelForm() {
    form(
        action = "/register-entity-model",
        method = FormMethod.post,
        classes = "entity-model-form"
    ) {
        textInput(name = "modelName") {
            placeholder = "Model Name"
        }
        br()
        textInput(name = "modelVersion") {
            placeholder = "Model Version"
        }
        textArea {
            id = "entityModelJson"
            name = "entityModelJson"
            rows = "10"
            cols = "50"
            +"Paste or drag your entity model JSON here..." // Placeholder content that can be overwritten by the user
        }
        br()
        submitInput { value = "Register Entity Model" }
    }
    // Script to handle drag and drop for the textArea
    script {
        // language=javascript
        unsafe {
            +"""
                // Handle drag over event to prevent the default behavior
                document.getElementById('entityModelJson').ondragover = function(event) {
                    event.preventDefault(); // Necessary to allow drop
                };

                // Handle file drop event for the text area
                document.getElementById('entityModelJson').ondrop = function(event) {
                    event.preventDefault(); // Prevent default behavior

                    const file = event.dataTransfer.files[0]; // Get the first file from the drop
                    if (file) {
                        const reader = new FileReader(); // Create a file reader to read the file
                        reader.onload = function(event) {
                            const textArea = document.getElementById('entityModelJson');
                            if (textArea) {
                                textArea.value = event.target.result; // Set the file content as the value of the text area
                            }
                        };
                        reader.readAsText(file); // Read the file as text
                    }
                };
            """.trimIndent()
        }
    }
}
