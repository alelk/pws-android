package io.github.myorg.myapp.contract

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * The contract's own rules, checked against the contract's own source.
 *
 * `api:contract` is KMP and its `commonTest` compiles for `wasmJs`, where there is no
 * reflection — so this is a **jvmTest** and it reads the `.kt` files. Gradle runs a `Test`
 * task with the module directory as its working directory, which is what makes the relative
 * path below deterministic.
 */
private val contractSources: List<File> =
    File("src/commonMain/kotlin/io/github/myorg/myapp/contract")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

private val sourceTexts: List<String> = contractSources.map { it.readText() }

private val clientSources: List<File> =
    File("../client/src/commonMain/kotlin/io/github/myorg/myapp/client")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

class ContractConventionsTest :
    FunSpec({
        test("the source directory was found") {
            // Without this, every other test in the file passes vacuously on an empty list.
            contractSources.size shouldBe 39 // pin YOUR count: a silent drop in scanned files must fail
        }

        test("every sealed DTO declares its discriminator") {
            sealedTypesWithoutDiscriminator() shouldBe KNOWN_IMPLICIT_DISCRIMINATORS
        }

        test("every @SerialName value is snake_case") {
            serialNames().filterNot { it.matches(SNAKE_CASE) }.toSet() shouldBe KNOWN_PASCAL_CASE_VARIANTS
        }

        test("a request body is named <Verb><Noun>Request") {
            requestTypeNames().filterNot(::hasRequestName).toSet() shouldBe KNOWN_BODY_NAME_EXCEPTIONS
        }

        test("a response without a resource is named <Verb><Noun>Response") {
            nonResourceResponseTypeNames().filterNot { it.endsWith("Response") }.toSet() shouldBe
                KNOWN_RESPONSE_NAME_EXCEPTIONS
        }

        test("no type is both a request body and a response") {
            requestBodyRoleTypeNames().intersect(responseRoleTypeNames()) shouldBe KNOWN_DUAL_ROLE_DTOS
        }

        test("the optimistic-locking field is named version") {
            fieldNames().filter { it.endsWith("Version") }.toSet() shouldBe KNOWN_CAS_FIELD_EXCEPTIONS
        }

        test("the JSON policy is declared once") {
            // A second `Json { … }` anywhere in the contract, the client or the transport means
            // two policies again, and the second one always drifts. The only allowed declaration
            // is ApiJson's own file.
            jsonBuilderDeclarations() shouldBe setOf("common/ApiJson.kt")
        }

        test("every list route answers an envelope") {
            clientMethodsReturningBareLists() shouldBe emptySet()
        }

        test("the wire envelope does not reach the client's callers") {
            clientMethodsReturning("PageDto") shouldBe emptySet()
        }

        // What a body creates is named by the client (idempotent replay), and the field is called `id`.
        test("every creation body carries the client's id, except KNOWN_SERVER_ID_CREATES") {
            creationBodies().filterValues { "id" !in it }.keys shouldBe KNOWN_SERVER_ID_CREATES.keys
        }

        test("no request body names the id of what it creates otherwise") {
            requestBodyFields().filterValues { fields -> fields.any { it in OTHER_ID_NAMES } }.keys shouldBe emptySet()
        }

        test("one client interface per file, and no method has a body") {
            clientFilesWithSeveralInterfaces() shouldBe emptySet()
            clientInterfaceMethodsWithBodies() shouldBe emptySet()
        }
    })

private fun sealedTypesWithoutDiscriminator(): Set<String> =
    sourceTexts
        .flatMap { source ->
            val lines = source.lines()
            lines.mapIndexedNotNull { index, line ->
                val name = SEALED_TYPE.find(line)?.groupValues?.get(1) ?: return@mapIndexedNotNull null
                val annotations = lines.subList((index - 3).coerceAtLeast(0), index + 1).joinToString("\n")
                name.takeUnless {
                    name == "OptionalFieldDto" || "@JsonClassDiscriminator(\"type\")" in annotations
                }
            }
        }.toSet()

private fun jsonBuilderDeclarations(): Set<String> =
    JSON_POLICY_SOURCE_ROOTS
        .flatMap { (root, prefix) ->
            root
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" && "Json {" in it.readText() }
                .map { file -> prefix + file.relativeTo(root).invariantSeparatorsPath }
                .toList()
        }.toSet()

private fun serialNames(): List<String> =
    sourceTexts.flatMap { source -> SERIAL_NAME.findAll(source).map { it.groupValues[1] }.toList() }

private fun typeNames(): Set<String> =
    sourceTexts.flatMap { source -> TYPE_DECLARATION.findAll(source).map { it.groupValues[1] }.toList() }.toSet()

private fun requestTypeNames(): Set<String> = typeNames().filterTo(mutableSetOf()) { it.endsWith("Request") }

private fun requestBodyRoleTypeNames(): Set<String> = requestTypeNames()

private fun responseRoleTypeNames(): Set<String> =
    typeNames().filterTo(mutableSetOf()) { it.endsWith("Dto") || it.endsWith("Response") }

private fun nonResourceResponseTypeNames(): Set<String> =
    typeNames().filterTo(mutableSetOf()) {
        it.endsWith("Response") || it.endsWith("ResultDto") || it.endsWith("CreatedDto")
    }

private fun hasRequestName(name: String): Boolean = REQUEST_VERBS.any { name.startsWith(it) }

private fun fieldNames(): List<String> =
    sourceTexts.flatMap { source -> FIELD_DECLARATION.findAll(source).map { it.groupValues[1] }.toList() }

private fun clientMethodsReturningBareLists(): Set<String> = clientMethodsReturning("List")

private fun clientMethodsReturning(type: String): Set<String> =
    clientSources
        .flatMap { file ->
            CLIENT_METHOD_RETURN
                .findAll(file.readText())
                .filter { it.groupValues[2] == type }
                .map { "${file.name}:${it.groupValues[1]}" }
                .toList()
        }.toSet()

private fun clientFilesWithSeveralInterfaces(): Set<String> =
    clientSources
        .filter { CLIENT_INTERFACE.findAll(it.readText()).count() > 1 }
        .mapTo(mutableSetOf()) { it.name }

private fun clientInterfaceMethodsWithBodies(): Set<String> =
    clientSources
        .flatMap { file ->
            interfaceBodies(file.readText()).flatMap { body ->
                CLIENT_METHOD_WITH_BODY.findAll(body).map { "${file.name}:${it.groupValues[1]}" }.toList()
            }
        }.toSet()

private fun interfaceBodies(source: String): List<String> =
    CLIENT_INTERFACE
        .findAll(source)
        .mapNotNull { declaration ->
            val opening = source.indexOf('{', declaration.range.last + 1).takeIf { it >= 0 } ?: return@mapNotNull null
            var depth = 0
            for (index in opening until source.length) {
                when (source[index]) {
                    '{' -> depth++
                    '}' -> if (--depth == 0) return@mapNotNull source.substring(opening + 1, index)
                }
            }
            null
        }.toList()

private val SEALED_TYPE = Regex("""\bsealed\s+(?:class|interface)\s+(\w+)""")
private val SERIAL_NAME = Regex("""@SerialName\("([^\"]+)"\)""")
private val TYPE_DECLARATION = Regex("""\b(?:data\s+)?(?:sealed\s+)?(?:class|interface)\s+([A-Z]\w*)""")
private val FIELD_DECLARATION = Regex("""\bval\s+(\w+)\s*:""")
private val CLIENT_INTERFACE = Regex("""(?m)^interface\s+\w+""")
private val CLIENT_METHOD_RETURN =
    Regex("""suspend\s+fun\s+(\w+)\s*\([\s\S]*?\)\s*:\s*Either<ApiError,\s*(List|PageDto)<""")
private val CLIENT_METHOD_WITH_BODY =
    Regex("""suspend\s+fun\s+(\w+)\s*\([\s\S]*?\)\s*:\s*Either<ApiError,[^\n]+>\s*(?:=|\{)""")
private val SNAKE_CASE = Regex("^[a-z][a-z0-9_]*$")

private val JSON_POLICY_SOURCE_ROOTS =
    listOf(
        File("src/commonMain/kotlin/io/github/myorg/myapp/contract") to "",
        File("../client/src/commonMain/kotlin/io/github/myorg/myapp/client") to "client/",
        File("../../server/transport/src/main/kotlin/io/github/myorg/myapp/server/transport") to "transport/",
    )

private val REQUEST_VERBS =
    setOf(
        // The verbs your requests may start with — reviewed like vocabulary.
        "Accept", "Change", "Confirm", "Create", "Decide", "Invite", "Login", "Place", "Post",
        "Refresh", "Register", "Reorder", "Request", "Restore", "Set", "Start", "Submit", "Switch", "Update", "Verify",
    )

/** Every polymorphic hierarchy says which key carries its discriminator. */
private val KNOWN_IMPLICIT_DISCRIMINATORS = emptySet<String>()

/** A discriminator is never a Kotlin class name again. */
private val KNOWN_PASCAL_CASE_VARIANTS = emptySet<String>()

/** A body says what it asks for. */
private val KNOWN_BODY_NAME_EXCEPTIONS = emptySet<String>()

/** Responses named neither `…Dto` nor `…Response`. */
private val KNOWN_RESPONSE_NAME_EXCEPTIONS = emptySet<String>()

/** A single type used as both a request body and a response. */
private val KNOWN_DUAL_ROLE_DTOS = emptySet<String>()

/** CAS fields not called `version`. */
private val KNOWN_CAS_FIELD_EXCEPTIONS = emptySet<String>()

/** `data class <Name>(…)` -> the names of its constructor's `val`s, for every class of the contract. */
private fun constructorFields(): Map<String, Set<String>> =
    sourceTexts
        .flatMap { source ->
            DATA_CLASS.findAll(source).map { it.groupValues[1] to source.balancedFrom(it.range.last) }
        }.associate { (name, params) -> name to FIELD_DECLARATION.findAll(params).map { it.groupValues[1] }.toSet() }

/**
 * The text between the `(` at [open] and its matching `)`. A naive `\(.*?\)` stops at the first `)`
 * of a KDoc inside the parameter list and silently drops the fields after it.
 */
private fun String.balancedFrom(open: Int): String {
    var depth = 0
    for (i in open until length) {
        when (this[i]) {
            '(' -> depth++
            ')' -> if (--depth == 0) return substring(open + 1, i)
        }
    }
    return substring(open + 1)
}

/** The bodies of a `POST` that creates something. */
private fun creationBodies(): Map<String, Set<String>> = constructorFields().filterKeys { CREATION_BODY.matches(it) }

/** Every `…Request` body, plus the variants of a sealed request whose class name does not end in `Request`. */
private fun requestBodyFields(): Map<String, Set<String>> =
    constructorFields().filterKeys { it.endsWith("Request") || it in REQUEST_VARIANTS }

private val DATA_CLASS = Regex("""data\s+class\s+(\w+)\s*\(""")
private val CREATION_BODY = Regex("""(Create|Invite|Register|Submit|Post|Adjust)\w*Request""")

/** The id of the thing being created is `id`, never `reportId`/`messageId`/`entryId`. */
private val OTHER_ID_NAMES = setOf("reportId", "messageId", "entryId")

/** Variants of a sealed transition request (`ItemTransitionRequest.Start`, …). */
private val REQUEST_VARIANTS = setOf<String>()

/** Creations whose id is the server's — before a tenant is chosen the client names no ids. Body -> why. */
private val KNOWN_SERVER_ID_CREATES: Map<String, String> =
    mapOf(
        "RegisterAccountRequest" to "no tenant yet; the server names the account",
        "CreateTenantRequest" to "no tenant yet; the server names the tenant",
    )
