package jthijs

import com.google.protobuf.InvalidProtocolBufferException
import io.quarkus.test.junit.QuarkusTest
import io.vertx.mqtt.MqttClientOptions
import java.nio.charset.Charset
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.random.Random
import org.eclipse.tahu.message.SparkplugBPayloadDecoder
import org.eclipse.tahu.message.SparkplugBPayloadEncoder
import org.eclipse.tahu.message.model.Metric
import org.eclipse.tahu.message.model.MetricDataType
import org.eclipse.tahu.message.model.SparkplugBPayload
import org.eclipse.tahu.message.model.Template
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@QuarkusTest
class TestMqttWillMessage {

    @ParameterizedTest
    @ValueSource(strings = [
        "UTF-8",
        "UTF-16",
        "UTF-16BE",
        "UTF-16LE",
        "US-ASCII",
//        "ISO-8859-1", // only this one seems to work
        "UTF-32",
        "UTF-32LE",
        "UTF-32BE"
    ])
    fun `just a reproducer`(chartSetName: String) {
        val encoder = SparkplugBPayloadEncoder()
        val decoder = SparkplugBPayloadDecoder()

        val template = Template.TemplateBuilder()
            .addMetric(
                Metric.MetricBuilder("my template metric", MetricDataType.Double, Random.nextDouble()).createMetric()
            )
            .addMetric(
                Metric.MetricBuilder("my template metric2", MetricDataType.String, "some metric").createMetric()
            )
            .createTemplate()

        val metrics = listOf(
            Metric.MetricBuilder("my will metric", MetricDataType.Double, Random.nextDouble()).createMetric(),
            Metric.MetricBuilder("another will metric", MetricDataType.Template, template).createMetric(),
        )

        val willMessagePayload = SparkplugBPayload(
            Date.from(Instant.now()),
            metrics,
            0,
            UUID.randomUUID().toString(),
            null
        )

        val charSet = Charset.forName(chartSetName)
        val willMessage = encoder.getBytes(willMessagePayload, false)
        val willMessageString = String(willMessage, charSet)

        val options = MqttClientOptions().apply {
            this.willMessage = willMessageString
        }

        val decodeMessageSucceed = decoder.buildFromByteArray(willMessage, null)
        Assertions.assertEquals(willMessagePayload.uuid, decodeMessageSucceed.uuid)

        Assertions.assertThrows(InvalidProtocolBufferException::class.java) {
            val decodedMessageFail = decoder.buildFromByteArray(options.willMessage.toByteArray(charSet), null)
            Assertions.assertEquals(willMessagePayload.uuid, decodedMessageFail.uuid)
        }
    }
}