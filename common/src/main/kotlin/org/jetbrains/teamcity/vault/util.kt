package org.jetbrains.teamcity.vault

import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.VersionComparatorUtil
import org.jetbrains.teamcity.vault.support.MappingJackson2HttpMessageConverter
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.vault.client.VaultClients
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.client.VaultHttpHeaders
import org.springframework.vault.config.ClientHttpRequestFactoryFactory
import org.springframework.vault.support.ClientOptions
import org.springframework.vault.support.SslConfiguration
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriTemplateHandler
import java.net.URI


fun isJava8OrNewer(): Boolean {
    return VersionComparatorUtil.compare(System.getProperty("java.specification.version"), "1.8") >= 0
}

fun createRestTemplate(settings: VaultFeatureSettings): RestTemplate {
    val endpoint = VaultEndpoint.from(URI.create(settings.url))!!
    val factory = ClientHttpRequestFactoryFactory.create(ClientOptions(), SslConfiguration.NONE)!!
    // HttpComponents.usingHttpComponents(options, sslConfiguration)

    return createRestTemplate(endpoint, factory)
}

fun createRestTemplate(endpoint: VaultEndpoint, factory: ClientHttpRequestFactory): RestTemplate {
    val template = createRestTemplate()

    template.requestFactory = factory
    template.uriTemplateHandler = createUriTemplateHandler(endpoint)

    return template
}

fun RestTemplate.withVaultToken(token: String): RestTemplate {
    this.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
        request.headers.add(VaultHttpHeaders.VAULT_TOKEN, token)
        execution.execute(request, body)
    })
    return this
}


fun createRestTemplate(): RestTemplate {
    // Like in org.springframework.vault.client.VaultClients.createRestTemplate()
    // However custom Jackson2 converter is used
    val converters = listOf<HttpMessageConverter<*>>(
            ByteArrayHttpMessageConverter(),
            StringHttpMessageConverter(),
            MappingJackson2HttpMessageConverter()
    )
    val template = RestTemplate(converters)
    template.interceptors.add(ClientHttpRequestInterceptor { request, body, execution -> execution.execute(request, body) })
    return template
}

fun isShouldSetEnvParameters(parameters: MutableMap<String, String>) = parameters[VaultConstants.BehaviourParameters.ExposeEnvParameters]?.toBoolean() ?: true

fun isShouldSetConfigParameters(parameters: MutableMap<String, String>) = parameters[VaultConstants.BehaviourParameters.ExposeConfigParameters]?.toBoolean() ?: false

private fun createUriTemplateHandler(endpoint: VaultEndpoint): DefaultUriTemplateHandler {
    val baseUrl = String.format("%s://%s:%s/%s/", endpoint.scheme, endpoint.host, endpoint.port, "v1")
    val handler = object : VaultClients.PrefixAwareUriTemplateHandler() {
        // For Spring 4.2 compatibility
        override fun expand(uriTemplate: String, uriVariables: MutableMap<String, *>?): URI {
            return super.expand(prepareUriTemplate(uriTemplate), uriVariables)
        }

        override fun expand(uriTemplate: String, vararg uriVariableValues: Any?): URI {
            return super.expand(prepareUriTemplate(uriTemplate), *uriVariableValues)
        }

        private fun prepareUriTemplate(uriTemplate: String): String {
            if (getBaseUrl() != null) {
                if (uriTemplate.startsWith("/") && getBaseUrl().endsWith("/")) {
                    return uriTemplate.substring(1)
                }

                if (!uriTemplate.startsWith("/") && !getBaseUrl().endsWith("/")) {
                    return "/" + uriTemplate
                }

                return uriTemplate
            }

            if (!uriTemplate.startsWith("/")) {
                return "/" + uriTemplate
            }
            return uriTemplate
        }
    }

    handler.baseUrl = baseUrl
    return handler
}

fun String?.nullIfEmpty(): String? {
    return StringUtil.nullIfEmpty(this)
}

fun String.ensureHasPrefix(prefix: String): String {
    return if (!this.startsWith(prefix)) "$prefix$this" else this
}

fun String.pluralize(size: Int) = StringUtil.pluralize(this, size)
fun String.pluralize(collection: Collection<Any>) = this.pluralize(collection.size)
fun String.sizeAndPluralize(collection: Collection<Any>) = "${collection.size} " + this.pluralize(collection)
