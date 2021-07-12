package de.culture4life.luca.document.provider.eudcc

import androidx.test.runner.AndroidJUnit4
import de.culture4life.luca.LucaUnitTest
import de.culture4life.luca.document.Document
import de.culture4life.luca.document.DocumentParsingException
import de.culture4life.luca.document.DocumentVerificationException
import de.culture4life.luca.document.provider.baercode.BaercodeTestResultProviderTest
import de.culture4life.luca.document.provider.opentestcheck.OpenTestCheckDocumentProviderTest
import de.culture4life.luca.registration.RegistrationData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(AndroidJUnit4::class)
class EudccDocumentProviderTest : LucaUnitTest() {

    companion object {
        private const val EUDCC_FULLY_VACCINATED = "HC1:6BFOXN*TS0BI\$ZD4N9:9S6RCVN5+O30K3/XIV0W23NTDEPWK G2EP4J0B3KLASMUG8GJL8LLG.3SA3/-2E%5VR5VVBJZILDBZ8D%JTQOL2009UVD0HX2JN*4CY009TX/9F/GZ%5U1MC82*%95HC2FCG2K80H-1GW\$5IKKQJO0OPN484SI4UUIMI.J9WVHWVH+ZE/T9MX1HRIWQHCR2HL9EIAESHOP6OH6MN9*QHAO96Y2/*13A5-8E6V59I9BZK6:IR/S09T./0LWTHC0/P6HRTO\$9KZ56DE/.QC\$QUC0:GOODPUHLO\$GAHLW 70SO:GOV636*2. KOKGKZGJMI:TU+MMPZ5OV1 V125VE-4RZ4E%5MK9BM57KPGX7K:7D-M1MO0Q2AQE:CA7ED6LF90I3DA+:E3OGJMSGX8+KL1FD*Y49+574MYKOE1MJ-69KKRB4AC8.C8HKK9NTYV4E1MZ3K1:HF.5E1MRB4WKP/HLIJL8JF8JF172M*8OEB2%7OREF:FO:7-WF11SKCU1MH8FWPVH%L635OBXTY*LPM6B9OBYSH:4Q1BQ:A5+I6:DQR9VKR8 BLHCFQMZA5:PHR14%GV4ZOP50\$ A 3"
        private const val EUDCC_PARTIALLY_VACCINATED = "HC1:6BF+70790T9WJWG.FKY*4GO0.O1CV2 O5 N2FBBRW1*70HS8WY04AC*WIFN0AHCD8KD97TK0F90KECTHGWJC0FDC:5AIA%G7X+AQB9746HS80:54IBQF60R6\$A80X6S1BTYACG6M+9XG8KIAWNA91AY%67092L4WJCT3EHS8XJC +DXJCCWENF6OF63W5NW6WF6%JC QE/IAYJC5LEW34U3ET7DXC9 QE-ED8%E.JCBECB1A-:8\$96646AL60A60S6Q\$D.UDRYA 96NF6L/5QW6307KQEPD09WEQDD+Q6TW6FA7C466KCN9E%961A6DL6FA7D46JPCT3E5JDLA7\$Q6E464W5TG6..DX%DZJC6/DTZ9 QE5\$CB\$DA/D JC1/D3Z8WED1ECW.CCWE.Y92OAGY8MY9L+9MPCG/D5 C5IA5N9\$PC5\$CUZCY\$5Y\$527BHB6*L8ARHDJL.Q7*2T7:SCNFZN70H6*AS6+T\$D9UCAD97R8NIBO+/RJVE\$9PAGPTBIZEP MO-Q0:R13IURRQ5MV93M9V3X2U:NDZSF"
        private const val EUDCC_TEST_RESULT = "HC1:6BFR%BH:7*I0PS33NUA9HWP5PZ2CLJ*GH7WV-UNA1VZJKZ6HX.A/5R..9*CV6+LJ*F.UN7A2BT8B+6B897S69R48S1.R1VJO9Q1ZZO+CC\$A9%T5X7RI25A8S57D JK-PQ+JR*FDTW3+1EC1JXLOQ58+KFL49ZMENAO.YOWR75PAH0HD6AIHCPWHJTF.RJ*JCSKEHL1N31HWEO67KJH8TIX-B3QB-+9*LCU:C:P2QEEQ7KF\$V--4CW7JWILDWU%Q%IO0LAK70J\$KW2JW56.KO8E2RHPH60ILI8T0N/7OEPD7P3+3IH9VZIVWP.44FX87QH5I97ZK0MK8OIGC3 3CQ6WO+9P9ECRSV%72M4L65 KAVKE*YPRHSIF1 89*4NDZ7FU6:F6NPJ1PHL059BGBB1%/C/J91R75Z5I7CWV0TREWYSY8ULK5HWPGEP\$SI5B1\$8HDOCH3JEBCL*8SE2AZT9SC+84JVGR39:2V*TR:KBW/4S:FK DOHF-1789MQ.18CV2C3YCN79OR176:1U:0CQVNGDJ0GUPO%CRT+QC/O\$:D/WQY\$3*5UR2M4YPFXK\$DH"
        private const val EUDCC_RECOVERED = "HC1:6BFOXN*TS0BI\$ZD-PHQ7I9AD66V5B22CH9M9ESI9XBHXK-%69LQOGI.*V76GCV4*XUA2P-FHT-HNTI4L6N\$Q%UG/YL WO*Z7ON15 BM0VM.JQ\$F4W17PG4.VAS5EG4V*BRL0K-RDY5RWOOH6PO9:TUQJAJG9-*NIRICVELZUZM9EN9-O9:PICIG805CZKHKB-43.E3KD3OAJ6*K6ZCY73JC3KD3ZQTWD3E.KLC8M3LP-89B9K+KB2KK3M*EDZI9\$JAQJKKIJX2MM+GWHKSKE MCAOI8%MCU5VTQDPIMQK9*O7%NC.UTWA6QK.-T3-SY\$NCU5CIQ 52744E09TBOC.UKMI\$8R+1A7CPFRMLNKNM8JI0JPGN:0K7OOBRLY667SYHJL9B7VPO:SWLH1/S4KQQK0\$5REQT5RN1FR%SHPLRKWJO8LQ84EBC\$-P4A0V1BBR5XWB3OCGEK:\$8HHOLQOZUJ*30Q8CD1"
        private const val EUDCC_RO_FULLY_VACCINATED = "HC1:NCFOXN%TSMAHN-HUSC7LDZL18ZT4 E/R8E:INDCCH184DCJ91YE0PPP-I.BDAZAF/8X*G3M9CXP3+AZW4%+A63HNNVR*G0C7PHBO33:X0 MBSQJ%F3KD3CU84Z0QPFSZ4NM0%*47%S%*48YIZ73423ZQTX63-E32R4UZ2 NVV5TN%2UP20J5/5LEBFD-48YI+T4D-4HRVUMNMD3323R1370RC-4A+2XEN QT QTHC31M3+E3CP456L X4CZKHKB-43.E3KD3OAJ5%IWZKRA38M7323 PCQP9-JNLBJ09BYY88EK:M2VW5Q41W63OH3TOOHJP7NVDEB\$/IL0J99SSZ4RZ4E%5MK96R96+PEN9C9Q9J1:.PNJPWH9 UPYF9Q/UIN9P8QOA9DIEF7F:-1G%5TW5A 6YO67N6D9ESJD2BFHXURIUC%55VTE:4IG9R6GCI69 52NF3TD%/2C4HABTDYVZGT%WMM4UG6M:5CNUR+54HVHAVLELA08MU1KMHFXLPVO4:7W3YB 20H3MW2"
        private const val EUDCC_BG_FULLY_VACCINATED = "HC1:NCFOXN*TS0BI\$ZDYSH-TTSBKCW1P-B0II6VL-36HD7-TM X4V7BI9CGJ99 PRAC/GPWBILC90GBYPLR-SCG1CSQ6U7SSQY%S5L51I0N:4UO5920R2EX:6N5FQ7EUN6/DKC3277I\$*SJAK9B9LGF9B9LW4G%89-8CNNG.8Q:GVD9B.OD4OYGFO-O%Z8JH1PCDJ*3TFH2V4IE9MIHJ6W48UK.GCY0\$2PH/MIE9WT0K3M9UVZSVV*001HW%8UE9.955B9-NT0 2\$\$0X4PCY0+-CVYCRMTB*05*9O%05\$K+8HYKSCPCXI00H3\$35+V4YC5/HQ1%RHCR+9AYDPEA7IB65C94JB*4Q2MVK1L98BQGCE*GV-27Y41BTW69 5B %S\$CNKDTDT7%K1IGGJXUEP5 PQ0CGYE91FD\$W4P90UC467WTNP8EFTHF+A4O6OM.6D1MR195QN%X47*KB*KYQTHFT4S8JYIVYF6C5*AD0SDW+J8WM%-3*LM2WFUNP/:GGABK8LN 0793*1PV%GT9BN+CYSDQ PT*4KBQ**P3RQ\$WK:TK60FR1KWKVV4PITNVYTHB0O-EHQN"
        private const val EUDCC_CY_TEST_RESULT = "HC1:6BFOXN%TSMAHN-H/N8KMQ8/8.:91 P/R84KF2FCIJ9+\$VZT78WAY5SV9TXTI5K4SA3/-2E%5G%5TW5A 6YO6XL6Q3QR\$P*NIV1JIZT-8B*ZJWFL8UJ8/BGOJ ZJ83B8\$TR63Y.TVBD3\$T*+3LTTLZI99JO.A3DJKYJ.ZJ08KZ0KYPIA+2/JT6%T\$.T08K5OI9YI:8D+FD%PD5DL%9DDAB2DNAHLW 70SO:GOLIROGO3T5ZXK9UO GOP*OSV8WP4R/5UYP.28XYQ/HQBCQU96:/6F0P3IRPS4V77ZJ82HPPEPHCR6W9RDOT*OYGO.20G%UBT1%OKPJA5EQJ-HVPI5\$0JCA1W4/GJI+C7*4M:KCY05B9QIPMB4O-O4IJXKTAMP8EF/HL*E1YE9/MVE1C3ZCH13H0D3ZCL4JMYAZ+S-A5\$XKX6TVTCZW4P-AK.GNNVR*G0C7/JBA93%A75HB:U8LI3FCN\$RV-Z88LJWBJ4DJ7PN*M04RVWJB0JNO572J6E+70ZSCDWS:NQZ0/TJ2%O6 A0%59-SA9MUOT8ZE9D7109T4I/BQWF1M42JZB:5DZY9DFVTSD4ET3%V:+COJU+ HKFLR2G73K/E73*A:DI"
        private const val EUDCC_FAKED_VACCINATION = "HC1:6BFOXN%TSMAHN-H+XO5XF7:UY%FJ.GO92\$:25B9O\$RZ CHJIACH.QQ-MPW\$NLEENKE\$JDVPL32KD0KSKE MCAOI8%M3/U8+S4-R9ZII%KP N7DS8+SX*OIO3Y9LZPK1\$I8%M0QIJ8CK.1Z2ACEIC.USMI92CD1D%09/-3T15K3449JP\$I/XK\$M8HK66YBCPC2L0:%OD3P5B9-NT0 2\$\$0X4PCY0+-CVYCDEBD0HX2JR\$4O1K.IA.C8KRDL4O54O4IGUJKJGI.IAHLCV5GVWNZIKXGG JMLII7EDTG90OA3DE0OARH9W/IO6AHCRTWA.DPN95*28+-OAC5G472N4GMK8C5H35N95ZTMV/M7755QLQQ5%YQ+GOOSPA1RP96RIR-ZOLOOBJ6NIR9SOC0P6YOH 71WR.RO91HVOR5*PDCMGAWRM3FGNR63%SA.26OA3W0FEF9\$JB04D/CHEHHOV3J.O*:A8QS6U3FM6HUEZXN.BS53GGOI96B:RKJTV VM39H430J2M/0"
    }

    private val provider = EudccDocumentProvider(application)

    @Test
    fun canParse_validData_emitsTrue() {
        provider.canParse(EUDCC_TEST_RESULT)
                .test().assertValue(true)
    }

    @Test
    fun canParse_invalidData_emitsFalse() {
        provider.canParse("anything")
                .test().assertValue(false)
    }

    @Test
    fun canParse_ticketIoDocument_emitsFalse() {
        provider.canParse(OpenTestCheckDocumentProviderTest.VALID_TEST_RESULT_TICKET_IO)
                .test().assertValue(false)
    }

    @Test
    fun canParse_baercodeDocument_emitsFalse() {
        provider.canParse(BaercodeTestResultProviderTest.TEST_QR_CODE)
                .test().assertValue(false)
    }

    @Test
    fun parse_fullyVaccinated_setsCorrectValues() {
        with(provider.parse(EUDCC_FULLY_VACCINATED).blockingGet().document) {
            assertEquals("Erika Dörte", firstName)
            assertEquals("Dießner Musterfrau", lastName)
            assertEquals(Document.TYPE_VACCINATION, type)
            assertEquals(Document.OUTCOME_FULLY_IMMUNE, outcome)
            assertEquals(1620345600000, testingTimestamp)
            assertEquals(1620345600000, resultTimestamp)
        }
    }

    @Test
    fun parse_romaniaFullyVaccinated_setsCorrectValues() {
        with(provider.parse(EUDCC_RO_FULLY_VACCINATED).blockingGet().document) {
            assertEquals("Teodor", firstName)
            assertEquals("Ion", lastName)
            assertEquals(Document.TYPE_VACCINATION, type)
            assertEquals(Document.OUTCOME_FULLY_IMMUNE, outcome)
        }
    }

    @Test
    fun validate_romaniaFullyVaccinated_completes() {
        with(provider.parse(EUDCC_RO_FULLY_VACCINATED).blockingGet()) {
            val registrationData = RegistrationData().apply {
                firstName = "TEODOR"
                lastName = "Ion"
            }
            provider.validate(this, registrationData).blockingAwait()
        }
    }

    @Test
    fun parse_bulgariaFullyVaccinated_setsCorrectValues() {
        with(provider.parse(EUDCC_BG_FULLY_VACCINATED).blockingGet().document) {
            assertEquals("СТАМО ГЕОРГИЕВ", firstName)
            assertEquals("ПЕТКОВ", lastName)
            assertEquals(Document.TYPE_VACCINATION, type)
            assertEquals(Document.OUTCOME_FULLY_IMMUNE, outcome)
        }
    }

    @Test
    fun parse_partiallyVaccinated_setsCorrectValues() {
        with(provider.parse(EUDCC_PARTIALLY_VACCINATED).blockingGet().document) {
            assertEquals("Erika", firstName)
            assertEquals("Mustermann", lastName)
            assertEquals("Robert Koch-Institut", labName)
            assertEquals(Document.TYPE_VACCINATION, type)
            assertEquals(Document.OUTCOME_PARTIALLY_IMMUNE, outcome)
            assertEquals("URN:UVCI:01DE/IZ12345A/5CWLU12RNOB9RXSEOP6FG8#W", hashableEncodedData)
        }
    }

    @Test
    fun parse_validTestData_setsCorrectValues() {
        val eudccDocument = provider.parse(EUDCC_TEST_RESULT).blockingGet()
        assertEquals(-170035200000, eudccDocument.document.dateOfBirth)  // "1964-08-12"
        with(eudccDocument.document) {
            assertEquals("Erika", firstName)
            assertEquals("Mustermann", lastName)
            assertEquals(Document.TYPE_FAST, type)
            assertEquals(Document.OUTCOME_NEGATIVE, outcome)
            assertEquals("Testzentrum Köln Hbf", labDoctorName)
            assertEquals("Robert Koch-Institut", labName)
            assertEquals(1622369542000, testingTimestamp)
            assertEquals(1622370615000, resultTimestamp)
            assertEquals("URN:UVCI:01DE/IZ12345A/5CWLU12RNOB9RXSEOP6FG8#W", hashableEncodedData)
        }
    }

    @Test
    fun parse_testDataWithSpecialDate_setsCorrectValues() {
        with(provider.parse(EUDCC_CY_TEST_RESULT).blockingGet().document) {
            assertEquals("Francisco", firstName)
            assertEquals("Garcia Miguel", lastName)
            assertEquals(1623060000110, testingTimestamp)
            assertEquals(1623061800701, resultTimestamp)
        }
    }

    @Test
    fun parse_recoveredCertificate_setsCorrectValues() {
        with(provider.parse(EUDCC_RECOVERED).blockingGet().document) {
            assertEquals("Erika", firstName)
            assertEquals("Mustermann", lastName)
            assertEquals("Robert Koch-Institut", labName)
            assertEquals(Document.TYPE_RECOVERY, type)
            assertEquals(Document.OUTCOME_FULLY_IMMUNE, outcome)
            assertEquals(1610236800000, testingTimestamp)
            assertEquals(1610236800000, resultTimestamp)
            assertEquals(1622246400000, validityStartTimestamp)
            assertEquals(1623715200000, expirationTimestamp)
            assertEquals("URN:UVCI:01DE/5CWLU12RNOB9RXSEOP6FG8#W", hashableEncodedData)
        }
    }

    @Test
    fun validate_correctName_completes() {
        val registrationData = RegistrationData().apply {
            firstName = "Erika"
            lastName = "Mustermann"
        }
        provider.parse(EUDCC_TEST_RESULT)
                .flatMapCompletable { result -> provider.validate(result, registrationData) }
                .test().assertComplete()
    }

    @Test
    fun validate_wrongName_fails() {
        val registrationData = RegistrationData().apply {
            firstName = "Hans"
            lastName = "Wurst"
        }
        provider.parse(EUDCC_TEST_RESULT)
                .flatMapCompletable { result -> provider.validate(result, registrationData) }
                .test().assertError(DocumentVerificationException::class.java)
    }

    @Test(expected = DocumentParsingException::class)
    fun validate_fakedDocument_fails() {
        provider.parse(EUDCC_FAKED_VACCINATION).test()
    }

    @Test
    fun parseDateAndTime_differentDateStyles_canParse() {
        assertEquals(1622370615000, "2021-05-30T10:30:15Z".parseDate())
        assertEquals(1623016799000, "2021-06-06T23:59:59+0200".parseDate())
    }
}