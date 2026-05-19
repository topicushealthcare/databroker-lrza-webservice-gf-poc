package nl.topicuszorg.databroker.lrzawebservice.service.synchronization

import ca.uhn.fhir.rest.client.api.IGenericClient
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.HealthcareService
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.OrganizationAffiliation
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Service

@Service
class InitialSyncService(
    private val lrzaFhirClient: IGenericClient,
    private val localReplicaFhirClient: IGenericClient,
)
{
    // Get all Care services from LRZa and sync to localreplica (ITI-90-NL)
    fun copyResourcesToLocalReplica()
    {
        syncAllResources(Endpoint::class.java)
        syncAllResources(Location::class.java)
        syncAllResources(HealthcareService::class.java)
        syncAllResources(Organization::class.java)
        syncAllResources(OrganizationAffiliation::class.java)
        syncAllResources(PractitionerRole::class.java)
        syncAllResources(Practitioner::class.java)
    }

    private fun <T : Resource> syncAllResources(resourceClass: Class<T>)
    {
        LOG.info("Starting initial sync for resource type ${resourceClass.simpleName}")
        try
        {
            var page = lrzaFhirClient.search<Bundle>()
                .forResource(resourceClass)
                .returnBundle(Bundle::class.java)
                .execute()

            while (page != null)
            {
                if (page.entry.isNotEmpty())
                {
                    convertToTransactionBundle(page)
                    localReplicaFhirClient.transaction().withBundle(page).execute()
                }
                page = page.getLink(Bundle.LINK_NEXT)?.let { lrzaFhirClient.loadPage().next(page).execute() }
            }
        }
        catch (e: Exception)
        {
            LOG.warn("Failed to copy resource type ${resourceClass.simpleName}: ${e.message}", e)
        }

        LOG.info("Completed initial sync for resource type ${resourceClass.simpleName}")
    }

    private fun convertToTransactionBundle(bundle: Bundle)
    {
        bundle.type = Bundle.BundleType.TRANSACTION
        bundle.entry.forEach { entry ->
            val resource = entry.resource
            val type = resource.fhirType()
            val id = resource.idElement.idPart
            entry.fullUrl = "$type/$id"
            entry.request.apply {
                method = Bundle.HTTPVerb.PUT
                url = "$type/$id"
            }
        }
    }

    companion object
    {
        private val LOG: Log = LogFactory.getLog(InitialSyncService::class.java)
    }
}