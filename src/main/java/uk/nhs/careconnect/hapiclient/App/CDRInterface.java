package uk.nhs.careconnect.hapiclient.App;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;

public class CDRInterface {

    FhirContext ctxR4 = FhirContext.forR4();
    FhirContext ctxSTU3 = FhirContext.forDstu3();
    IGenericClient client = ctxR4.newRestfulGenericClient("http://hapi.fhir.org/baseR4/");

    public CDRInterface(){
        client.setEncoding(EncodingEnum.XML);

    }

    public org.hl7.fhir.dstu3.model.Bundle getPatientBundle( String patientId) {


        Bundle patientBundle = client
                .search()
                .forResource(Patient.class)
                .where(Patient.RES_ID.exactly().code(patientId))
                .include(Patient.INCLUDE_GENERAL_PRACTITIONER)
                .include(Patient.INCLUDE_ORGANIZATION)
                .returnBundle(Bundle.class)
                .execute();

        return convertBundletoSTU3(patientBundle);
    }



    public org.hl7.fhir.dstu3.model.Bundle getEncounterBundleRev(String encounterId) {

        Bundle bundle = client
                .search()
                .forResource(Encounter.class)
                .where(Patient.RES_ID.exactly().code(encounterId))
                .revInclude(new Include("Condition:encounter"))
                .revInclude(new Include("Observation:encounter"))
                .revInclude(new Include("QuestionnaireResponse:encounter"))
                .revInclude(new Include("Task:encounter"))
                .revInclude(new Include("Procedure:encounter"))
                .include(new Include("*"))
                .count(100) // be careful of this TODO
                .returnBundle(Bundle.class)
                .execute();

        return convertBundletoSTU3(bundle);
    }

    private org.hl7.fhir.dstu3.model.Bundle convertBundletoSTU3(Bundle bundle) {
        org.hl7.fhir.dstu3.model.Bundle stu3Bundle = new org.hl7.fhir.dstu3.model.Bundle();
        for (Bundle.BundleEntryComponent entryComponent : bundle.getEntry()) {
            org.hl7.fhir.dstu3.model.Resource resourceSTU3 = (org.hl7.fhir.dstu3.model.Resource) ctxSTU3.newXmlParser().parseResource(ctxR4.newXmlParser().encodeResourceToString(entryComponent.getResource()));
            if (entryComponent.getResource() instanceof Encounter) {
                Encounter encounter = (Encounter) entryComponent.getResource();
                org.hl7.fhir.dstu3.model.Encounter encounterSTU3 = (org.hl7.fhir.dstu3.model.Encounter) resourceSTU3;
                if (encounter.hasServiceType()) {
                    encounterSTU3.setClass_(
                            new org.hl7.fhir.dstu3.model.Coding()
                                    .setCode(encounter.getServiceType().getCodingFirstRep().getCode())
                                    .setSystem(encounter.getServiceType().getCodingFirstRep().getSystem())
                                    .setDisplay(encounter.getServiceType().getCodingFirstRep().getDisplay())
                    );
                }
                resourceSTU3 = encounterSTU3;
            } if (entryComponent.getResource() instanceof Task) {
                Task task = (Task) entryComponent.getResource();
                org.hl7.fhir.dstu3.model.Task taskSTU3 = (org.hl7.fhir.dstu3.model.Task) resourceSTU3;
                if (task.hasReasonCode()) {
                    taskSTU3.setReason(new org.hl7.fhir.dstu3.model.CodeableConcept()
                            .addCoding(
                                    new org.hl7.fhir.dstu3.model.Coding()
                                            .setCode(task.getReasonCode().getCodingFirstRep().getCode())
                                            .setDisplay(task.getReasonCode().getCodingFirstRep().getDisplay())
                                            .setSystem(task.getReasonCode().getCodingFirstRep().getSystem())

                            )
                    );
                }
                resourceSTU3 = taskSTU3;
            }
            stu3Bundle.addEntry().setResource(resourceSTU3).setFullUrl(entryComponent.getFullUrl());

        }
        return stu3Bundle;
    }
    public org.hl7.fhir.dstu3.model.Bundle getConditionBundle(String patientId) {

        Bundle bundle = client
                .search()
                .forResource(Condition.class)
                .where(Condition.PATIENT.hasId(patientId))
                .and(Condition.CLINICAL_STATUS.exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();

        return convertBundletoSTU3(bundle);
    }

    public org.hl7.fhir.dstu3.model.Bundle getEncounterBundle(String patientId) {

        Bundle bundle =  client
                .search()
                .forResource(Encounter.class)
                .where(Encounter.PATIENT.hasId(patientId))
                .count(3) // Last 3 entries same as GP Connect
                .returnBundle(Bundle.class)
                .execute();
        return convertBundletoSTU3(bundle);
    }

    public org.hl7.fhir.dstu3.model.Organization getOrganization( String sdsCode) {
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
        return (org.hl7.fhir.dstu3.model.Organization) ctxSTU3.newXmlParser().parseResource(ctxR4.newXmlParser().encodeResourceToString(organization));
    }

    public org.hl7.fhir.dstu3.model.Bundle getMedicationStatementBundle(String patientId) {

        Bundle bundle = client
                .search()
                .forResource(MedicationStatement.class)
                .where(MedicationStatement.PATIENT.hasId(patientId))
                .and(MedicationStatement.STATUS.exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();
        return convertBundletoSTU3(bundle);
    }


    public org.hl7.fhir.dstu3.model.Bundle getMedicationRequestBundle(IGenericClient client,String patientId) {

        Bundle bundle = client
                .search()
                .forResource(MedicationStatement.class)
                .where(MedicationRequest.PATIENT.hasId(patientId))
                .and(MedicationRequest.STATUS.exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();
        return convertBundletoSTU3(bundle);
    }

    public org.hl7.fhir.dstu3.model.Bundle getAllergyBundle(String patientId) {

        Bundle bundle = client
                .search()
                .forResource(AllergyIntolerance.class)
                .where(AllergyIntolerance.PATIENT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();
        return convertBundletoSTU3(bundle);
    }

}
