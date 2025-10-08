package no.entur.geocoder.converter.osm

import crosby.binary.osmosis.OsmosisReader
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.task.v0_6.Sink
import java.io.File
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class OsmIterator(inputFile: File) : AbstractIterator<Entity>() {
    private val queue = LinkedBlockingQueue<Entity>()
    private val poisonPill: Entity = Node(CommonEntityData(-1L, 0, Date(0), null, 0L), 0.0, 0.0)

    private val poiKeys =
        setOf(
            "amenity", "shop", "tourism", "leisure", "historic", "office", "craft",
            "public_transport", "railway", "station", "aeroway", "natural", "waterway",
        )

    private fun isPoi(tags: Collection<org.openstreetmap.osmosis.core.domain.v0_6.Tag>) =
        tags.any { it.key == "name" } && tags.any { it.key in poiKeys }

    init {
        val reader = OsmosisReader(inputFile)
        reader.setSink(
            object : Sink {
                override fun initialize(metaData: MutableMap<String, Any>?) {}

                override fun process(entityContainer: EntityContainer?) {
                    entityContainer?.entity?.let { entity ->
                        if (isPoi(entity.tags)) {
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
}
