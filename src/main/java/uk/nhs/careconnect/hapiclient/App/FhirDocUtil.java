package uk.nhs.careconnect.hapiclient.App;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.utilities.xhtml.XhtmlDocument;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.hl7.fhir.utilities.xhtml.XhtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;

public class FhirDocUtil {

    public static final String SNOMEDCT = "http://snomed.info/sct";

    Context ctxThymeleaf = new Context();

    private TemplateEngine templateEngine;

    private XhtmlParser xhtmlParser = new XhtmlParser();

    private Bundle bundle = new Bundle();

    private static final Logger log = LoggerFactory.getLogger(FhirDocUtil.class);

    FhirDocUtil(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public Composition.SectionComponent getConditionSection(Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<Condition> conditions = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("887151000000100")
                .setDisplay("Problems and issues");
        section.setTitle("Problems and Issues");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Condition) {
                Condition condition = (Condition) entry.getResource();
                section.getEntry().add(new Reference("urn:uuid:"+condition.getId()));
                conditions.add(condition);
            }
        }
        ctxThymeleaf.clearVariables();
        ctxThymeleaf.setVariable("conditions", conditions);

        section.getText().setDiv(getDiv("condition")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getMedicationStatementSection(Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<MedicationStatement>  medicationStatements = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("933361000000108")
                .setDisplay("Medications and medical devices");
        section.setTitle("Medications and medical devices");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof MedicationStatement) {
                MedicationStatement medicationStatement = (MedicationStatement) entry.getResource();
                section.getEntry().add(new Reference("urn:uuid:"+medicationStatement.getId()));
                medicationStatements.add(medicationStatement);

            }
        }
        ctxThymeleaf.clearVariables();
        ctxThymeleaf.setVariable("medicationStatements", medicationStatements);

        section.getText().setDiv(getDiv("medicationStatement")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getAllergySection(Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<AllergyIntolerance>  allergyIntolerances = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("886921000000105")
                .setDisplay("Allergies and adverse reactions");
        section.setTitle("Allergies and adverse reactions");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof AllergyIntolerance) {
                AllergyIntolerance allergyIntolerance = (AllergyIntolerance) entry.getResource();
                section.getEntry().add(new Reference("urn:uuid:"+allergyIntolerance.getId()));
                allergyIntolerances.add(allergyIntolerance);
            }
        }
        ctxThymeleaf.clearVariables();

        ctxThymeleaf.setVariable("allergies", allergyIntolerances);

        section.getText().setDiv(getDiv("allergy")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getEncounterSection(Composition composition, Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();
        // TODO Get Correct code.
        ArrayList<Encounter>  encounters = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("713511000000103")
                .setDisplay("Encounter administration");
        section.setTitle("Encounter");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Encounter) {
                Encounter encounter = (Encounter) entry.getResource();
                section.getEntry().add(new Reference("urn:uuid:"+encounter.getId()));
                encounters.add(encounter);
            }
        }
        ctxThymeleaf.clearVariables();
        // This section should really be in composition
        ctxThymeleaf.setVariable("composition", composition);
        ctxThymeleaf.setVariable("encounters", encounters);

        section.getText().setDiv(getDiv("encounter")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getAttendanceDetailsSection(Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<QuestionnaireResponse>  questionnaireResponses = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem(SNOMEDCT)
                .setCode("1077881000000105")
                .setDisplay("Attendance details");
        section.setTitle("Consulation notes");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof QuestionnaireResponse) {
                QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) entry.getResource();
                for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : questionnaireResponse.getItem()){
                    item.getText();
                    for (QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer : item.getAnswer()) {
                      //  answer.getValueCoding().getDisplay()

                    }
                };
                section.getEntry().add(new Reference("urn:uuid:"+questionnaireResponse.getId()));
                questionnaireResponses.add(questionnaireResponse);
            }
        }
        ctxThymeleaf.clearVariables();
        ctxThymeleaf.setVariable("questionnaireResponses", questionnaireResponses);

        section.getText().setDiv(getDiv("questionnaireResponse")).setStatus(Narrative.NarrativeStatus.GENERATED);
        return section;
    }

    public Composition.SectionComponent getMedicationRequestSection(Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<MedicationRequest>  medicationRequests = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem(SNOMEDCT)
                .setCode("933361000000108")
                .setDisplay("Medications and medical devices");
        section.setTitle("Medications and medical devices");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest medicationRequest = (MedicationRequest) entry.getResource();
                //medicationStatement.getMedicationReference().getDisplay();
                section.getEntry().add(new Reference("urn:uuid:"+medicationRequest.getId()));
                medicationRequest.getAuthoredOn();
                medicationRequests.add(medicationRequest);

            }
        }
        ctxThymeleaf.clearVariables();
        ctxThymeleaf.setVariable("medicationRequests", medicationRequests);

        section.getText().setDiv(getDiv("medicationRequest")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getOrganisation(Bundle bundle, String ODSCode, Coding coding, String title) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        Organization organisation = null;

        section.getCode().addCoding(coding);
        section.setTitle(title);

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Organization) {
                Organization organization= (Organization) entry.getResource();
                if (organization.getIdentifierFirstRep().getValue().equals(ODSCode)) {
                    section.getEntry().add(new Reference("urn:uuid:" + organization.getId()));
                    organisation = organization;
                }
            }
        }
        ctxThymeleaf.clearVariables();

        if (organisation != null) {
            ctxThymeleaf.setVariable("organisation", organisation);
            section.getText().setDiv(getDiv("organisation")).setStatus(Narrative.NarrativeStatus.GENERATED);
        }
        return section;
    }

    public Composition.SectionComponent getObservationSection(Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<Observation>  observations = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem(SNOMEDCT)
                .setCode("425044008")
                .setDisplay("Physical exam section");
        section.setTitle("Observations");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Observation) {
                Observation observation= (Observation) entry.getResource();

                section.getEntry().add(new Reference("urn:uuid:"+observation.getId()));

                observations.add(observation);
            }
        }
        ctxThymeleaf.clearVariables();

        ctxThymeleaf.setVariable("observations", observations);

        section.getText().setDiv(getDiv("observation")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getProcedureSection(Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<Procedure>  procedures = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem(SNOMEDCT)
                .setCode("887171000000109")
                .setDisplay("Procedues");
        section.setTitle("Procedures");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Procedure) {
                Procedure procedure = (Procedure) entry.getResource();

                section.getEntry().add(new Reference("urn:uuid:"+procedure.getId()));
                procedures.add(procedure);
            }
        }
        ctxThymeleaf.clearVariables();

        ctxThymeleaf.setVariable("procedures", procedures);

        section.getText().setDiv(getDiv("procedure")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getPlansActionsSection(Bundle bundle) {
        this.bundle = bundle;
        Composition.SectionComponent section = new Composition.SectionComponent();

        ArrayList<Task> tasks = new ArrayList<>();

        section.getCode().addCoding()
                .setSystem(SNOMEDCT)
                .setCode("887201000000105")
                .setDisplay("Plan and requested actions");
        section.setTitle("Plan and requested actions");

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Task) {
                Task task = (Task) entry.getResource();

                section.getEntry().add(new Reference("urn:uuid:"+task.getId()));
                tasks.add(task);
            }
        }
        ctxThymeleaf.clearVariables();

        ctxThymeleaf.setVariable("tasks", tasks);
        ctxThymeleaf.setVariable("util", this);

        section.getText().setDiv(getDiv("task")).setStatus(Narrative.NarrativeStatus.GENERATED);

        return section;
    }

    public Composition.SectionComponent getProvenaceSection(Composition composition,Bundle bundle) {
        Composition.SectionComponent section = new Composition.SectionComponent();

        section.getCode().addCoding()
                .setSystem(SNOMEDCT)
                .setCode("887231000000104")
                .setDisplay("Person completing record");
        section.setTitle("Consultation Participants");
        Practitioner practitioner = null;
        Organization organization = null;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (composition.getCustodian().getReference().equals(entry.getFullUrl()) && entry.getResource() instanceof Organization ) {
                organization = (Organization) entry.getResource();
                section.getEntry().add(new Reference("urn:uuid:"+organization.getId()));
            }
            if (composition.getAuthorFirstRep().getReference().equals(entry.getFullUrl()) && entry.getResource() instanceof Practitioner ) {
                practitioner= (Practitioner) entry.getResource();
                section.getEntry().add(new Reference("urn:uuid:"+practitioner.getId()));
            }
        }
        ctxThymeleaf.clearVariables();

        ctxThymeleaf.setVariable("practitioner", practitioner);

        ctxThymeleaf.setVariable("organisation", organization);
        XhtmlNode nodePractitioner = getDiv("practitioner");
        XhtmlNode nodeOrganisation = getDiv("organisation");
       try {
           XhtmlDocument parsed = xhtmlParser.parse("<div>"
                   + nodePractitioner.getValueAsString()
                   + nodeOrganisation.getValueAsString()
                   + "</div>", null);
           XhtmlNode xhtmlNode = parsed.getDocumentElement();
           section.getText().setDiv(xhtmlNode).setStatus(Narrative.NarrativeStatus.GENERATED);
       } catch (Exception ex) {
           log.error(ex.getMessage());
       }
        return section;
    }

    public Patient generatePatientHtml(Patient patient, Bundle fhirDocument) {


        ctxThymeleaf.clearVariables();
        ctxThymeleaf.setVariable("patient", patient);
        for (Bundle.BundleEntryComponent entry : fhirDocument.getEntry()) {
            if (entry.getResource() instanceof Practitioner) ctxThymeleaf.setVariable("gp", entry.getResource());
            if (entry.getResource() instanceof Organization) ctxThymeleaf.setVariable("practice", entry.getResource());
            Practitioner practice;

        }

        patient.getText().setDiv(getDiv("patient")).setStatus(Narrative.NarrativeStatus.GENERATED);
        log.debug(patient.getText().getDiv().getValueAsString());

        return patient;
    }

    public Composition generateCompositionHtml(Composition composition) {

        ctxThymeleaf.clearVariables();
        ctxThymeleaf.setVariable("composition", composition);

        composition.getText().setDiv(getDiv("composition")).setStatus(Narrative.NarrativeStatus.GENERATED);
        return composition;
    }

    private XhtmlNode getDiv(String template) {
        XhtmlNode xhtmlNode = null;
        String processedHtml = templateEngine.process(template, ctxThymeleaf);
        try {
            XhtmlDocument parsed = xhtmlParser.parse(processedHtml, null);
            xhtmlNode = parsed.getDocumentElement();
            log.debug(processedHtml);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return xhtmlNode;
    }

    public String getDisplay(Reference reference) {
        if (reference.hasDisplay()) return reference.getDisplay();
        if (reference.hasReference()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (reference.getReference().equals(entry.getFullUrl())) {
                    if (entry.getResource() instanceof Organization) {
                        return ((Organization) entry.getResource()).getName();
                    }
                    if (entry.getResource() instanceof Practitioner) {
                        return ((Practitioner) entry.getResource()).getNameFirstRep().getNameAsSingleString();
                    }
                }
            }
        }
        return "";
    }

}
