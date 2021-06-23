package de.culture4life.luca.document.provider.baercode;

import de.culture4life.luca.document.Document;
import de.culture4life.luca.document.DocumentParsingException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import androidx.test.runner.AndroidJUnit4;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class BaercodeDocumentTest {

    @Test(expected = DocumentParsingException.class)
    public void checkProcedures_forEmptyProcedures_fails() throws DocumentParsingException {
        ArrayList<Procedure> procedures = new ArrayList<>();
        BaercodeDocument.check(procedures);
    }

    @Test(expected = DocumentParsingException.class)
    public void checkProcedures_forMixedProcedureTypes_fails() throws DocumentParsingException {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.PCR_TEST, 1621589912));
        procedures.add(new Procedure(Procedure.Type.VACCINATION_VAXZEVRIA, 1621589912));
        BaercodeDocument.check(procedures);
    }

    @Test
    public void getOutcome_forNegativePCRTest_isNegative() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.PCR_TEST, 1621589912));
        Assert.assertEquals(Document.OUTCOME_NEGATIVE, BaercodeDocument.getOutcome(procedures, false));
    }

    @Test
    public void getOutcome_forPositivePCRTest_isPositive() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.PCR_TEST, 1621589912));
        Assert.assertEquals(Document.OUTCOME_POSITIVE, BaercodeDocument.getOutcome(procedures, true));
    }

    @Test
    public void getOutcome_forNegativeFastTest_isNegative() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.ANTIGEN_FAST_TEST, 1621589912));
        Assert.assertEquals(Document.OUTCOME_NEGATIVE, BaercodeDocument.getOutcome(procedures, false));
    }

    @Test
    public void getOutcome_forPositiveFastTest_isPositive() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.ANTIGEN_FAST_TEST, 1621589912));
        Assert.assertEquals(Document.OUTCOME_POSITIVE, BaercodeDocument.getOutcome(procedures, true));
    }

    @Test
    public void getOutcome_forJannsenVaccination_isFullyVaccinated() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.VACCINATION_JANNSEN, 1621589912));
        Assert.assertEquals(Document.OUTCOME_FULLY_IMMUNE, BaercodeDocument.getOutcome(procedures, true));
    }

    @Test
    public void getOutcome_forEmptyProcedures_isUnknown() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        Assert.assertEquals(Document.OUTCOME_UNKNOWN, BaercodeDocument.getOutcome(procedures, true));
    }

    @Test
    public void getOutcome_forComirnatyVaccination_isPartiallyVaccinated() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.VACCINATION_COMIRNATY, 1621589912));
        Assert.assertEquals(Document.OUTCOME_PARTIALLY_IMMUNE, BaercodeDocument.getOutcome(procedures, true));
    }

    @Test
    public void getOutcome_forComirnatyVaccination_isFullyVaccinated() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.VACCINATION_COMIRNATY, 1621589912));
        procedures.add(new Procedure(Procedure.Type.VACCINATION_COMIRNATY, 1619861912));
        Assert.assertEquals(Document.OUTCOME_FULLY_IMMUNE, BaercodeDocument.getOutcome(procedures, true));
    }

    @Test
    public void getOutcome_forThreeComirnatyVaccinations_isFullyVaccinated() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.VACCINATION_COMIRNATY, 1631589912));
        procedures.add(new Procedure(Procedure.Type.VACCINATION_COMIRNATY, 1621589912));
        procedures.add(new Procedure(Procedure.Type.VACCINATION_COMIRNATY, 1619861912));
        Assert.assertEquals(Document.OUTCOME_FULLY_IMMUNE, BaercodeDocument.getOutcome(procedures, true));
    }

    @Test
    public void getOutcome_forCombinedVaccination_isFullyVaccinated() {
        ArrayList<Procedure> procedures = new ArrayList<>();
        procedures.add(new Procedure(Procedure.Type.VACCINATION_COMIRNATY, 1621589912));
        procedures.add(new Procedure(Procedure.Type.VACCINATION_VAXZEVRIA, 1619861912));
        Assert.assertEquals(Document.OUTCOME_FULLY_IMMUNE, BaercodeDocument.getOutcome(procedures, true));
    }

}