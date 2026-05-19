package nl.topicuszorg.databroker.lrzawebservice.service.administration

import ca.uhn.fhir.rest.client.api.IGenericClient
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.OrganizationAffiliation
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Service

@Service
class OrganizationService(
    private val lrzaFhirClient: IGenericClient,
)
{
    fun createZorgaanbieder(orgName: String, ura: String, type: String): String
    {
        val organization = Organization().apply {
            meta = Meta().apply {
                addProfile("http://minvws.github.io/generiekefuncties-docs/StructureDefinition/nl-gf-organization")
            }
            addIdentifier().apply {
                use = Identifier.IdentifierUse.OFFICIAL
                system = "http://fhir.nl/fhir/NamingSystem/ura"
                value = ura
            }

            addType().addCoding().apply {
                system = "http://nictiz.nl/fhir/NamingSystem/organization-type"
                code = type
            }
            name = orgName
        }

        val outcome = lrzaFhirClient.create().resource(organization).execute()
        val created = outcome.resource as Organization
        val createdId = created.idElement.idPart

        LOG.info("Created Zorgaanbieder Organization with id: $createdId")

        return createdId
    }

    fun createITVendor(orgName: String, kvk: String): String
    {
        val organization = Organization().apply {
            meta = Meta().apply {
                addProfile("http://minvws.github.io/generiekefuncties-docs/StructureDefinition/nl-gf-organization")
            }
            addIdentifier().apply {
                use = Identifier.IdentifierUse.OFFICIAL
                system = "http://fhir.nl/fhir/NamingSystem/kvk"
                value = kvk
            }
            addType().addCoding().apply {
                system = "http://minvws.github.io/generiekefuncties-docs/CodeSystem/nl-gf-sbi-cs"
                code = "6210"
                display = "Computer programming activities"
            }
            name = orgName
        }

        val outcome = lrzaFhirClient.create().resource(organization).execute()
        val created = outcome.resource as Organization
        val createdId = created.idElement.idPart

        LOG.info("Created IT-Vendor Organization with id: $createdId")

        return createdId
    }

    fun createAffiliation(vendorId: String, zorgaanbiederId: String, affiliationId: String): String
    {
        val affiliation = OrganizationAffiliation().apply {
            meta = Meta().apply {
                addProfile("http://minvws.github.io/generiekefuncties-docs/StructureDefinition/nl-gf-organizationaffiliation")
            }
            addIdentifier().apply {
                use = Identifier.IdentifierUse.OFFICIAL
                system = "urn:ietf:rfc:3986"
                value = affiliationId
            }
            active = true
            organization = Reference("Organization/$zorgaanbiederId")
            participatingOrganization = Reference("Organization/$vendorId")
            addCode().codingFirstRep.apply {
                system = "http://minvws.github.io/generiekefuncties-docs/CodeSystem/nl-gf-authorization-type-cs"
                code = "lrza-careprovider-admin"
                display = "LRZa Care Provider Administration"
            }
        }

        val outcome = lrzaFhirClient.create().resource(affiliation).execute()
        val created = outcome.resource as OrganizationAffiliation
        val createdId = created.idElement.idPart

        LOG.info("Created OrganizationAffiliation with id: $createdId")

        return createdId
    }

    companion object
    {
        private val LOG: Log = LogFactory.getLog(OrganizationService::class.java)
    }
}