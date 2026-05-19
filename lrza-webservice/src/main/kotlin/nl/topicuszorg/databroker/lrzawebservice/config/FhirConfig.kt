package nl.topicuszorg.databroker.lrzawebservice.config

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.okhttp.client.OkHttpRestfulClientFactory
import ca.uhn.fhir.rest.client.api.IGenericClient
import nl.topicuszorg.databroker.lrzawebservice.service.KeystoreService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
class FhirConfig
{
    @Value("\${application.lrza.base-url}")
    private lateinit var lrzaFhirBaseUrl: String

    @Value("\${application.local-replica.base-url}")
    private lateinit var localReplicaUrl: String

    @Value("\${application.mtls.keystore-file}")
    private lateinit var keystoreFile: Resource

    @Value("\${application.mtls.keystore-password}")
    private lateinit var keystorePassword: String

    @Bean
    fun fhirContext(): FhirContext
    {
        val fhirContext = FhirContext.forR4().apply {
            restfulClientFactory = OkHttpRestfulClientFactory(this)
        }
        val mTLSClient = KeystoreService.createMtlsHttpClient(keystoreFile, keystorePassword)
        fhirContext.restfulClientFactory.setHttpClient(mTLSClient)

        return fhirContext
    }

    @Bean(name = ["lrzaFhirClient"])
    fun lrzaFhirClient(fhirContext: FhirContext): IGenericClient
    {
        return fhirContext.newRestfulGenericClient(lrzaFhirBaseUrl)
    }

    @Bean(name = ["localReplicaFhirClient"])
    fun localReplicaFhirClient(fhirContext: FhirContext): IGenericClient
    {
        return fhirContext.newRestfulGenericClient(localReplicaUrl)
    }
}