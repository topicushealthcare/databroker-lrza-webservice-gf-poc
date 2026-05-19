package nl.topicuszorg.databroker.lrzawebservice.service.synchronization

import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.HealthcareService
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.OrganizationAffiliation
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PeriodicSyncService(
    private val lrzaFhirClient: IGenericClient,
    private val localReplicaFhirClient: IGenericClient,
)
{
    // Get all Care services from LRZa and sync to localreplica (ITI-90-NL)
    fun updateLocalReplica(since: OffsetDateTime)
    {
        syncResourcesSince(Endpoint::class.java, since)
        syncResourcesSince(Location::class.java, since)
        syncResourcesSince(HealthcareService::class.java, since)
        syncResourcesSince(Organization::class.java, since)
        syncResourcesSince(OrganizationAffiliation::class.java, since)
        syncResourcesSince(PractitionerRole::class.java, since)
        syncResourcesSince(Practitioner::class.java, since)
    }

    private fun <T : Resource> syncResourcesSince(resourceClass: Class<T>, since: OffsetDateTime)
    {
        val entries = fetchHistory(resourceClass, since)
        if (entries.isEmpty())
        {
            LOG.info("No history entries found for resource type ${resourceClass.simpleName} since $since")
            return
        }

        val (upserts, deletes) = buildTransactions(entries)

        if (upserts.entry.isEmpty())
        {
            LOG.info("No updates detected for resource type ${resourceClass.simpleName} since $since")
        }
        else
        {
            LOG.info("Executing ${upserts.entry.size} upsert transactions for ${resourceClass.simpleName}")
            localReplicaFhirClient.transaction()
                .withBundle(upserts)
                .execute()
        }

        if (deletes.entry.isEmpty())
        {
            LOG.info("No deletes detected for resource type ${resourceClass.simpleName} since $since")
        }
        else
        {
            LOG.info("Executing ${deletes.entry.size} delete transactions for ${resourceClass.simpleName}")
            localReplicaFhirClient.transaction()
                .withBundle(deletes)
                .execute()
        }
    }

    private fun <T : Resource> fetchHistory(
        resourceClass: Class<T>,
        since: OffsetDateTime
    ): List<Bundle.BundleEntryComponent>
    {
        val sinceInstant = InstantType(since.toInstant().toString())

        LOG.info("Fetching history for resource type ${resourceClass.simpleName} since $sinceInstant")

        var page = lrzaFhirClient.history()
            .onType(resourceClass)
            .returnBundle(Bundle::class.java)
            .since(sinceInstant)
            .execute()

        val result = mutableListOf<Bundle.BundleEntryComponent>()

        while (page != null)
        {
            result += page.entry

            page = page.getLink(Bundle.LINK_NEXT)
                ?.let { lrzaFhirClient.loadPage().next(page).execute() }
        }

        return result
    }

    private fun buildTransactions(
        entries: List<Bundle.BundleEntryComponent>
    ): Pair<Bundle, Bundle>
    {
        val upserts = mutableListOf<Bundle.BundleEntryComponent>()
        val deletes = mutableListOf<Bundle.BundleEntryComponent>()

        entries.forEach { entry ->
            when (entry.request.method)
            {
                Bundle.HTTPVerb.POST,
                Bundle.HTTPVerb.PUT    -> createUpsertEntry(entry)?.let(upserts::add)

                Bundle.HTTPVerb.DELETE -> createDeleteEntry(entry)?.let(deletes::add)

                else -> LOG.info("Unrecognized request for ${entry.request.method} ${entry.request.url}")
            }
        }

        val upsertBundle = Bundle().apply {
            entry = upserts.distinctBy { it.request.url }
            type = Bundle.BundleType.TRANSACTION
        }

        val deleteBundle = Bundle().apply {
            entry = deletes
            type = Bundle.BundleType.TRANSACTION
        }

        return upsertBundle to deleteBundle
    }

    private fun createUpsertEntry(
        historyEntry: Bundle.BundleEntryComponent
    ): Bundle.BundleEntryComponent?
    {
        val resource = historyEntry.resource
        val id = resource.idElement.idPart
        val type = resource.fhirType()

        val latest = fetchLatest(resource, id) ?: return null

        return Bundle.BundleEntryComponent().apply {
            this.resource = latest
            request = Bundle.BundleEntryRequestComponent().apply {
                method = Bundle.HTTPVerb.PUT
                url = "$type/$id"
            }
        }
    }

    private fun fetchLatest(resource: Resource, id: String): Resource?
    {
        return try
        {
            lrzaFhirClient.read()
                .resource(resource.javaClass)
                .withId(id)
                .execute()
        }
        catch (e: BaseServerResponseException)
        {
            if (e.statusCode == 404 || e.statusCode == 410)
            {
                LOG.warn("${resource.resourceType} with id $id update is skipped because resource was already deleted")
                return null
            }
            throw e
        }
    }

    private fun createDeleteEntry(
        historyEntry: Bundle.BundleEntryComponent
    ): Bundle.BundleEntryComponent?
    {
        val url = historyEntry.request.url ?: return null

        return Bundle.BundleEntryComponent().apply {
            request = Bundle.BundleEntryRequestComponent().apply {
                method = Bundle.HTTPVerb.DELETE
                this.url = url
            }
        }
    }

    companion object
    {
        private val LOG: Log = LogFactory.getLog(PeriodicSyncService::class.java)
    }
}