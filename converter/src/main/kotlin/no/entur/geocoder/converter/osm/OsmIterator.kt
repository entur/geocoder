package no.entur.geocoder.converter.osm

import crosby.binary.osmosis.OsmosisReader
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.domain.v0_6.Relation
import org.openstreetmap.osmosis.core.task.v0_6.Sink
import java.io.File
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * Iterator for OSM PBF files with optional filtering.
 *
 * @param inputFile The OSM PBF file to read
 * @param filter Optional filter predicate. If null, all entities are returned (unfiltered mode).
 *               If provided, only entities matching the predicate are returned.
 */
class OsmIterator(inputFile: File, private val filter: ((Entity) -> Boolean)? = null) : AbstractIterator<Entity>() {
    private val queue = LinkedBlockingQueue<Entity>()
    private val poisonPill: Entity = Node(CommonEntityData(-1L, 0, Date(0), null, 0L), 0.0, 0.0)

    init {
        val reader = OsmosisReader(inputFile)
        reader.setSink(
            object : Sink {
                override fun initialize(metaData: MutableMap<String, Any>?) {}

                override fun process(entityContainer: EntityContainer?) {
                    entityContainer?.entity?.let { entity ->
                        if (filter == null || filter.invoke(entity)) {
                            queue.put(entity)
                        }
                    }
                }

                override fun complete() {
                    queue.put(poisonPill)
                }

                override fun close() {}
            },
        )
        Thread { reader.run() }.start()
    }

    override fun computeNext() {
        val entity = queue.take()
        if (entity === poisonPill) {
            done()
        } else {
            setNext(entity)
        }
    }

    companion object {
        /** Filter for POI entities (entities with name and wanted tags) */
        val POI_FILTER: (Entity) -> Boolean = { entity ->
            entity.tags.any { it.key == "name" } &&
                    entity.tags.any { tag -> OSMPopularityCalculator.hasFilter(tag.key, tag.value) }
        }

        /** Filter for administrative boundary relations only */
        val ADMIN_BOUNDARY_FILTER: (Entity) -> Boolean = { entity ->
            if (entity is Relation) {
                val tags = entity.tags.associate { it.key to it.value }
                tags["boundary"] == "administrative" &&
                        tags["admin_level"] in listOf(
                    AdministrativeBoundaryIndex.ADMIN_LEVEL_COUNTY.toString(),
                    AdministrativeBoundaryIndex.ADMIN_LEVEL_MUNICIPALITY.toString()
                )
            } else {
                false
            }
        }

        /** Filter for ways only (used when collecting way node IDs) */
        val WAY_FILTER: (Entity) -> Boolean = { entity ->
            entity is org.openstreetmap.osmosis.core.domain.v0_6.Way
        }

        /** Filter for nodes only (used when collecting node coordinates) */
        val NODE_FILTER: (Entity) -> Boolean = { entity ->
            entity is org.openstreetmap.osmosis.core.domain.v0_6.Node
        }
    }
}
