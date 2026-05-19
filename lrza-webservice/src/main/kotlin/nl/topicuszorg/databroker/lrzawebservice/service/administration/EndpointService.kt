package nl.topicuszorg.databroker.lrzawebservice.service.administration

import ca.uhn.fhir.rest.client.api.IGenericClient
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostEndpointRequestJson
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.HealthcareService
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Service

@Service
class EndpointService(
    private val lrzaFhirClient: IGenericClient
    )
{
    fun createEndpoint(postEndpointRequestJson: PostEndpointRequestJson): String
    {
        val endpoint = Endpoint().apply {
            meta = Meta().apply {
                addProfile("http://minvws.github.io/generiekefuncties-docs/StructureDefinition/nl-gf-endpoint")
            }
            addIdentifier().apply {
                use = Identifier.IdentifierUse.OFFICIAL
                system = "urn:ietf:rfc:3986"
                value = postEndpointRequestJson.endpointIdentifier
            }
            status = Endpoint.EndpointStatus.ACTIVE
            connectionType = Coding().apply {
                system = postEndpointRequestJson.connectionType.system
                code = postEndpointRequestJson.connectionType.code
            }
            name = postEndpointRequestJson.endpointName
            managingOrganization = Reference("Organization/${postEndpointRequestJson.vendorId}")
            addPayloadType().apply {
                coding = listOf(Coding().apply {
                    system = postEndpointRequestJson.payloadType.system
                    code = postEndpointRequestJson.payloadType.code
                })
            }
            address = postEndpointRequestJson.addressUrl
        }

        val outcome = lrzaFhirClient.create().resource(endpoint).execute()
        val created = outcome.resource as Endpoint
        val createdId = created.idElement.idPart

        LOG.info("Created Endpoint with id: $createdId")

        return createdId
    }

    fun linkEndpointToOrganization(zorgaanbiederId: String, endpointId: String)
    {
        try
        {
            val organization = lrzaFhirClient.read()
                .resource(Organization::class.java)
                .withId(zorgaanbiederId)
                .execute()

            organization.addEndpoint(Reference("Endpoint/$endpointId"))

            lrzaFhirClient.update().resource(organization).execute()

            LOG.info("Linked Endpoint $endpointId to Organization $zorgaanbiederId")
        }
        catch (e: Exception)
        {
            LOG.warn("Failed to link Endpoint $endpointId to Organization $zorgaanbiederId: ${e.message}", e)
            return
        }
    }

    fun linkEndpointToHealthcareService(healthcareServiceId: String, endpointId: String)
    {
        try
        {
            val healthcareService = lrzaFhirClient.read()
                .resource(HealthcareService::class.java)
                .withId(healthcareServiceId)
                .execute()

            healthcareService.addEndpoint(Reference("Endpoint/$endpointId"))

            lrzaFhirClient.update().resource(healthcareService).execute()

            LOG.info("Linked Endpoint $endpointId to HealthcareService $healthcareServiceId")
        }
        catch (e: Exception)
        {
            LOG.warn("Failed to link Endpoint $endpointId to Organization $healthcareServiceId: ${e.message}", e)
            return
        }
    }

    companion object
    {
        private val LOG: Log = LogFactory.getLog(EndpointService::class.java)
    }
}