package nl.topicuszorg.databroker.lrzawebservice.controller

import nl.topicuszorg.databroker.lrzawebservice.api.generated.api.SynchronizationApi
import nl.topicuszorg.databroker.lrzawebservice.service.synchronization.InitialSyncService
import nl.topicuszorg.databroker.lrzawebservice.service.synchronization.PeriodicSyncService
import org.apache.juli.logging.LogFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SynchronizationController(
    private val initialSyncService: InitialSyncService,
    private val periodicSyncService: PeriodicSyncService
) : SynchronizationApi
{
    override fun getInitialSync(): ResponseEntity<Unit>
    {
        LOG.info("Starting initial sync")
        initialSyncService.copyResourcesToLocalReplica()
        LOG.info("Initial sync completed")

        return ResponseEntity.ok().build()
    }

    override fun getPeriodicSync(since: OffsetDateTime): ResponseEntity<Unit>
    {
        LOG.info("Starting periodic sync with changes since: $since")
        periodicSyncService.updateLocalReplica(since)
        LOG.info("Periodic sync completed")

        return ResponseEntity.ok().build()
    }


    companion object
    {
        private val LOG = LogFactory.getLog(SynchronizationController::class.java)
    }
}