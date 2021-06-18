package de.culture4life.luca.testing.provider.eudcc

import de.culture4life.luca.testing.TestResultParsingException
import de.culture4life.luca.testing.provider.TestResultProvider
import dgca.verifier.app.decoder.DefaultCertificateDecoder
import dgca.verifier.app.decoder.DefaultCertificateDecoder.Companion.PREFIX
import dgca.verifier.app.decoder.DefaultCertificateDecoder.Companion.decodeCose
import dgca.verifier.app.decoder.DefaultCertificateDecoder.Companion.decompressBase45DecodedData
import dgca.verifier.app.decoder.base45.Base45Decoder
import io.reactivex.rxjava3.core.Single

/**
 * Provider for the EU Digital COVID Certificate (EUDCC)
 */
class EudccResultProvider : TestResultProvider<EudccResult>() {
    private val base45Decoder = Base45Decoder()
    private val decoder = DefaultCertificateDecoder(base45Decoder)

    override fun canParse(encodedData: String): Single<Boolean> {
        return Single.fromCallable {
            try {
                val withoutPrefix = if (encodedData.startsWith(PREFIX)) encodedData.drop(PREFIX.length) else encodedData
                val decompressed = base45Decoder.decode(withoutPrefix).decompressBase45DecodedData()
                val cbor = decompressed.decodeCose().cbor
                EudccSchemaValidator().validate(cbor)
            } catch (t: Throwable) {
                false
            }
        }
    }

    override fun parse(encodedData: String): Single<EudccResult> {
        return Single.just(EudccResult(encodedData, decoder.decodeCertificate(encodedData)))
            .onErrorResumeNext { throwable ->
                if (throwable is TestResultParsingException) {
                    Single.error<TestResultParsingException>(throwable)
                }
                Single.error(TestResultParsingException(throwable))
            }
    }
}