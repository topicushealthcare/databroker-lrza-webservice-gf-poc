package nl.topicuszorg.databroker.lrzawebservice.service.administration

import ca.uhn.fhir.rest.client.api.IGenericClient
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Service

@Service
class DeviceService(
    private val lrzaFhirClient: IGenericClient,
)
{
    fun createDevice(applicationId: String, applicationName: String, vendorId: String): String
    {
        val device = Device().apply {
            meta = Meta().apply {
                addProfile("http://minvws.github.io/generiekefuncties-docs/StructureDefinition/nl-gf-device")
            }
            addDeviceName().apply {
                name = applicationName
                type = Device.DeviceNameType.USERFRIENDLYNAME
            }
            addIdentifier().apply {
                use = Identifier.IdentifierUse.OFFICIAL
                system = "urn:ietf:rfc:3986"
                value = applicationId
            }
            owner = Reference("Organization/${vendorId}")
        }

        val outcome = lrzaFhirClient.create().resource(device).execute()
        val created = outcome.resource as Device
        val createdId = created.idElement.idPart

        LOG.info("Created Application/Device with id: $createdId")

        return createdId
    }

    companion object
    {
        private val LOG: Log = LogFactory.getLog(DeviceService::class.java)
    }
}