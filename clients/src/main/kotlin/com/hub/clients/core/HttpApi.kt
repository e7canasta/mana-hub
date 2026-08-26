package com.hub.clients.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ManaHubException(val statusCode: Int, val body: String) :
    RuntimeException("HTTP $statusCode: ${body.take(200)}")

class HttpApi(private val baseUrl: String) {

    private val mapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val client: HttpClient = HttpClient.newHttpClient()

    fun <T : Any> get(path: String, type: Class<T>): T =
        execute(newRequest(path, "GET"), type)

    fun <T : Any> post(path: String, body: Any, type: Class<T>): T =
        execute(newRequest(path, "POST", body), type)

    fun postVoid(path: String, body: Any) {
        val response = client.send(newRequest(path, "POST", body), HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() < 400) { throw ManaHubException(response.statusCode(), response.body()) }
    }

    fun <T : Any> patch(path: String, body: Any, type: Class<T>): T =
        execute(newRequest(path, "PATCH", body), type)

    fun <T : Any> put(path: String, body: Any, type: Class<T>): T =
        execute(newRequest(path, "PUT", body), type)

    fun delete(path: String) {
        val response = client.send(newRequest(path, "DELETE"), HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() < 400) { throw ManaHubException(response.statusCode(), response.body()) }
    }

    private fun newRequest(path: String, method: String, body: Any? = null): HttpRequest {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Accept", "application/json")
        if (body != null) {
            builder.header("Content-Type", "application/json")
            builder.method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
        }
        return builder.build()
    }

    private fun <T : Any> execute(request: HttpRequest, type: Class<T>): T {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 400) {
            throw ManaHubException(response.statusCode(), response.body())
        }
        return mapper.readValue(response.body(), type)
    }
}
