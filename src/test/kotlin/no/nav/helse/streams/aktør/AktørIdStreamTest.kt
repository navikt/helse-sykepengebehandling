package no.nav.helse.streams.aktør

import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Environment
import no.nav.helse.streams.JsonDeserializer
import no.nav.helse.streams.JsonSerializer
import no.nav.helse.streams.Topics
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*


class AktørIdStreamTest {

    @Test
    fun `that aktørId is added to message`() {
        val aktørregisterClientMock = mockk<AktørregisterClient>()
        every {
            aktørregisterClientMock.gjeldendeNorskIdent("1573082186699")
        } returns "12345678911"

        val config = Properties()
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "sykepengebehandling")
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
        val testDriver = TopologyTestDriver(AktørIdStream(Environment(), aktørregisterClientMock).fromSyfo(), config)

        val factory = ConsumerRecordFactory<String, JSONObject>(Topics.SYKEPENGESØKNADER_INN.name, StringSerializer(), JsonSerializer())

        val record = JSONObject("""{"name": "Ole Hansen", "aktorId": "1573082186699", "soknadstype": "typen", "status": "sendt"}""")
        testDriver.pipeInput(factory.create(record))

        val outputRecord = testDriver.readOutput(Topics.SYKEPENGEBEHANDLING.name, StringDeserializer(), JsonDeserializer())

        Assertions.assertEquals("12345678911", outputRecord.value().getString("norskIdent"))
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun `tear it down`() {
            CollectorRegistry.defaultRegistry.clear()
        }
    }
}
