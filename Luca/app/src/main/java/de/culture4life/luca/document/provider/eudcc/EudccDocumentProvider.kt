package de.culture4life.luca.document.provider.eudcc

import android.content.Context
import de.culture4life.luca.R
import de.culture4life.luca.document.DocumentParsingException
import de.culture4life.luca.document.provider.DocumentProvider
import dgca.verifier.app.decoder.DefaultCertificateDecoder
import dgca.verifier.app.decoder.DefaultCertificateDecoder.Companion.PREFIX
import dgca.verifier.app.decoder.base45.Base45Decoder
import dgca.verifier.app.decoder.decodeCose
import dgca.verifier.app.decoder.decompressBase45DecodedData
import io.reactivex.rxjava3.core.Single

/**
 * Provider for the EU Digital COVID Certificate (EUDCC)
 */
@kotlin.ExperimentalUnsignedTypes
class EudccDocumentProvider(val context: Context) : DocumentProvider<EudccDocument>() {

    private val base45Decoder = Base45Decoder()
    private val decoder = DefaultCertificateDecoder(base45Decoder)

    override fun canParse(encodedData: String): Single<Boolean> {
        return Single.fromCallable {
            val withoutPrefix = if (encodedData.startsWith(PREFIX)) {
                encodedData.drop(PREFIX.length)
            } else {
                encodedData
            }
            val decompressed = base45Decoder.decode(withoutPrefix).decompressBase45DecodedData()
            val cbor = decompressed.decodeCose().cbor
            EudccSchemaValidator().validate(cbor)
        }.onErrorReturn { false }
    }

    override fun parse(encodedData: String): Single<EudccDocument> {
        return Single.fromCallable {
            EudccDocument(encodedData, decoder.decodeCertificate(encodedData))
        }
            .map { it.document.provider = context.getString(R.string.provider_name_eu_dcc); it }
            .onErrorResumeNext { throwable ->
                if (throwable is DocumentParsingException) {
                    Single.error<DocumentParsingException>(throwable)
                }
                Single.error(DocumentParsingException(throwable))
            }
    }

}