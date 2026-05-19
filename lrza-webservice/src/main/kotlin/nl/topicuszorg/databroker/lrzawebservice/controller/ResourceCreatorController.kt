package nl.topicuszorg.databroker.lrzawebservice.controller

import nl.topicuszorg.databroker.lrzawebservice.api.generated.api.AdministrationApi
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostApplicationRequestJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostEndpointRequestJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostHealthcareServiceRequestJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostOrganization201ResponseJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostOrganizationAffiliationRequestJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostOrganizationCreateItVendorRequestJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PostOrganizationRequestJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PutHealthcareServiceRequestJson
import nl.topicuszorg.databroker.lrzawebservice.api.generated.model.PutOrganizationRequestJson
import nl.topicuszorg.databroker.lrzawebservice.service.administration.DeviceService
import nl.topicuszorg.databroker.lrzawebservice.service.administration.EndpointService
import nl.topicuszorg.databroker.lrzawebservice.service.administration.OrganizationService
import nl.topicuszorg.databroker.lrzawebservice.service.administration.HealthcareServiceService
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ResourceCreatorController(
    private val healthcareServiceService: HealthcareServiceService,
    private val organizationService: OrganizationService,
    private val endpointService: EndpointService,
    private val deviceService: DeviceService

) : AdministrationApi
{
    override fun postOrganization(postOrganizationRequestJson: PostOrganizationRequestJson): ResponseEntity<PostOrganization201ResponseJson>
    {
        LOG.info("Creating Zorgaanbieder Organization")
        val id = organizationService.createZorgaanbieder(postOrganizationRequestJson.name,
                                                         postOrganizationRequestJson.ura,
                                                         postOrganizationRequestJson.type)
        val response = PostOrganization201ResponseJson(id)

        return ResponseEntity.ok(response)
    }

    override fun postOrganizationCreateItVendor(postOrganizationCreateItVendorRequestJson: PostOrganizationCreateItVendorRequestJson): ResponseEntity<PostOrganization201ResponseJson>
    {
        LOG.info("Creating It-Vendor Organization")
        val id = organizationService.createITVendor(postOrganizationCreateItVendorRequestJson.name,
                                                    postOrganizationCreateItVendorRequestJson.kvk)
        val response = PostOrganization201ResponseJson(id)

        return ResponseEntity.ok(response)
    }

    override fun postOrganizationAffiliation(postOrganizationAffiliationRequestJson: PostOrganizationAffiliationRequestJson): ResponseEntity<PostOrganization201ResponseJson>
    {
        LOG.info("Creating OrganizationAffiliation")
        val id = organizationService.createAffiliation(postOrganizationAffiliationRequestJson.vendorId,
                                                       postOrganizationAffiliationRequestJson.zorgaanbiederId, postOrganizationAffiliationRequestJson.affiliationIdentifier)
        val response = PostOrganization201ResponseJson(id)

        return ResponseEntity.ok(response)
    }

    override fun postApplication(postApplicationRequestJson: PostApplicationRequestJson): ResponseEntity<PostOrganization201ResponseJson>
    {
        LOG.info("Creating Device/Application")
        val id = deviceService.createDevice(postApplicationRequestJson.applicationIdentifier,
                                                       postApplicationRequestJson.applicationName,
                                                       postApplicationRequestJson.vendorId)
        val response = PostOrganization201ResponseJson(id)

        return ResponseEntity.ok(response)
    }

    override fun postEndpoint(postEndpointRequestJson: PostEndpointRequestJson): ResponseEntity<PostOrganization201ResponseJson>
    {
        LOG.info("Creating Endpoint")
        val id = endpointService.createEndpoint(postEndpointRequestJson)
        val response = PostOrganization201ResponseJson(id)

        return ResponseEntity.ok(response)
    }

    override fun postHealthcareService(postHealthcareServiceRequestJson: PostHealthcareServiceRequestJson): ResponseEntity<PostOrganization201ResponseJson>
    {
        LOG.info("Creating HealthcareService")
        val id = healthcareServiceService.createHealthcareService(postHealthcareServiceRequestJson)
        val response = PostOrganization201ResponseJson(id)

        return ResponseEntity.ok(response)
    }

    override fun putOrganization(putOrganizationRequestJson: PutOrganizationRequestJson): ResponseEntity<Unit>
    {
        LOG.info("Linking Endpoint to Organization")
        endpointService.linkEndpointToOrganization(
            putOrganizationRequestJson.zorgaanbiederId,
            putOrganizationRequestJson.endpointId
        )

        return ResponseEntity.ok().build()
    }

    override fun putHealthcareService(putHealthcareServiceRequestJson: PutHealthcareServiceRequestJson): ResponseEntity<Unit>
    {
        LOG.info("Linking Endpoint to HealthcareService")
        endpointService.linkEndpointToHealthcareService(
            putHealthcareServiceRequestJson.serviceId,
            putHealthcareServiceRequestJson.endpointId
        )

        return ResponseEntity.ok().build()
    }

    companion object
    {
        private val LOG: Log = LogFactory.getLog(ResourceCreatorController::class.java)
    }
}