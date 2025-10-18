package no.entur.geocoder.converter.osm

import crosby.binary.osmosis.OsmosisReader
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.domain.v0_6.Tag
import org.openstreetmap.osmosis.core.task.v0_6.Sink
import java.io.File
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class OsmIterator(inputFile: File) : AbstractIterator<Entity>() {
    private val queue = LinkedBlockingQueue<Entity>()
    private val poisonPill: Entity = Node(CommonEntityData(-1L, 0, Date(0), null, 0L), 0.0, 0.0)

    private fun hasWantedTag(tags: Collection<Tag>) =
        tags.any { it.key == "name" } &&
        tags.any { tag -> Poi.isWantedTag(tag.key, tag.value) }

    init {
        val reader = OsmosisReader(inputFile)
        reader.setSink(
            object : Sink {
                override fun initialize(metaData: MutableMap<String, Any>?) {}

                override fun process(entityContainer: EntityContainer?) {
                    entityContainer?.entity?.let { entity ->
                        if (hasWantedTag(entity.tags)) {
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
