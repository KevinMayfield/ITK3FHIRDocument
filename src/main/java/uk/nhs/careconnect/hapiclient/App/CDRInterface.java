package uk.nhs.careconnect.hapiclient.App;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.*;

public class CDRInterface {

    FhirContext ctxR4 = FhirContext.forR4();

    IGenericClient client = ctxR4.newRestfulGenericClient("http://hapi.fhir.org/baseR4/");

    public CDRInterface(){
        client.setEncoding(EncodingEnum.XML);

    }

    public Bundle getPatientBundle( String patientId) {


        Bundle patientBundle = client
                .search()
                .forResource(Patient.class)
                .where(Patient.RES_ID.exactly().code(patientId))
                .include(Patient.INCLUDE_GENERAL_PRACTITIONER)
                .include(Patient.INCLUDE_ORGANIZATION)
                .returnBundle(Bundle.class)
                .execute();

        return patientBundle;
    }


    public Bundle getEncounterBundleRev( String encounterId) {

        Bundle bundle = client
                .search()
                .forResource(Encounter.class)
                .where(Patient.RES_ID.exactly().code(encounterId))
                .revInclude(new Include("*"))
                .include(new Include("*"))
                .count(100) // be careful of this TODO
                .returnBundle(Bundle.class)
                .execute();
        return bundle;
    }
    public Bundle getConditionBundle(String patientId) {

        return client
                .search()
                .forResource(Condition.class)
                .where(Condition.PATIENT.hasId(patientId))
                .and(Condition.CLINICAL_STATUS.exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();
    }
    public Bundle getEncounterBundle(String patientId) {

        return client
                .search()
                .forResource(Encounter.class)
                .where(Encounter.PATIENT.hasId(patientId))
                .count(3) // Last 3 entries same as GP Connect
                .returnBundle(Bundle.class)
                .execute();
    }

    public Organization getOrganization( String sdsCode) {
        Organization organization = null;
        Bundle bundle =  client
                .search()
                .forResource(Organization.class)
                .where(Organization.IDENTIFIER.exactly().code(sdsCode))

                .returnBundle(Bundle.class)
                .execute();
        if (bundle.getEntry().size()>0) {
            if (bundle.getEntry().get(0).getResource() instanceof Organization)
                organization = (Organization) bundle.getEntry().get(0).getResource();
        }
        return organization;
    }
    public Bundle getMedicationStatementBundle(String patientId) {

        return client
                .search()
                .forResource(MedicationStatement.class)
                .where(MedicationStatement.PATIENT.hasId(patientId))
                .and(MedicationStatement.STATUS.exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();
    }

    public Bundle getMedicationRequestBundle(IGenericClient client,String patientId) {

        return client
                .search()
                .forResource(MedicationStatement.class)
                .where(MedicationRequest.PATIENT.hasId(patientId))
                .and(MedicationRequest.STATUS.exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();
    }

    public Bundle getAllergyBundle(String patientId) {

        return client
                .search()
                .forResource(AllergyIntolerance.class)
                .where(AllergyIntolerance.PATIENT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();
    }
}
