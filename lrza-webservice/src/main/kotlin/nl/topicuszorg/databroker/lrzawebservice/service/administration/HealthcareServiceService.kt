package nl.topicuszorg.databroker.lrzawebservice.service.administration

import ca.uhn.fhir.rest.client.api.IGenericClient
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostHealthcareServiceRequestJson
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.HealthcareService
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Service

@Service
class HealthcareServiceService(
    private val lrzaFhirClient: IGenericClient,
)
{
    fun createHealthcareService(postHealthcareServiceRequestJson: PostHealthcareServiceRequestJson): String
    {
        val healthcareService = HealthcareService().apply {
            meta = Meta().apply {
                addProfile("http://minvws.github.io/generiekefuncties-docs/StructureDefinition/nl-gf-healthcareservice")
            }
            addIdentifier().apply {
                use = Identifier.IdentifierUse.OFFICIAL
                system = "urn:ietf:rfc:3986"
                value = postHealthcareServiceRequestJson.serviceIdentifier
            }
            active = true
            addType().apply {
                coding = listOf(Coding().apply {
                    system = postHealthcareServiceRequestJson.type.system
                    code = postHealthcareServiceRequestJson.type.code
                })
            }
            name = postHealthcareServiceRequestJson.name
            providedBy = Reference("Organization/${postHealthcareServiceRequestJson.zorgaanbiederId}")

            addSpecialty().apply {
                coding = listOf(Coding().apply {
                    system = postHealthcareServiceRequestJson.specialty?.system
                    code = postHealthcareServiceRequestJson.specialty?.code
                })
            }
        }

        val outcome = lrzaFhirClient.create().resource(healthcareService).execute()
        val created = outcome.resource as HealthcareService
        val createdId = created.idElement.idPart

        LOG.info("Created HealthcareService with id: $createdId")

        return createdId
    }

    companion object
    {
        private val LOG: Log = LogFactory.getLog(HealthcareServiceService::class.java)
    }
}