package de.culture4life.luca.testing.provider.eudcc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.upokecenter.cbor.CBORObject
import dgca.verifier.app.decoder.JSON_SCHEMA_V1
import dgca.verifier.app.decoder.cwt.CwtHeaderKeys
import timber.log.Timber


/**
 * Schema validator for EU Digital COVID Certificate (EUDCC)
 */
class EudccSchemaValidator {

    private val mapper = ObjectMapper()

    private fun getJsonSchemaFromStringContent(schemaContent: String?): JsonSchema? {
        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
        return factory.getSchema(schemaContent)
    }

    fun validate(cbor: ByteArray): Boolean {
        val schema: JsonSchema = getJsonSchemaFromStringContent(JSON_SCHEMA_V1)!!
        var isValid = false
        try {
            val map = CBORObject.DecodeFromBytes(cbor)
            val hcert = map[CwtHeaderKeys.HCERT.asCBOR()]
            val json = hcert[CBORObject.FromObject(1)].ToJSONString()

            val jsonNode: JsonNode = mapper.readTree(json)

            val errors = schema.validate(jsonNode)
            isValid = errors.isEmpty()
        } catch (ex: Exception) {
            Timber.e("Exception while validating EUDCC: ${ex.message}")
        }

        return isValid
    }
}