package uk.nhs.careconnect.hapiclient.App;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.thymeleaf.TemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootApplication
public class FhirDocumentApp implements CommandLineRunner {

    @Autowired
    private TemplateEngine templateEngine;

    private static final Logger log = LoggerFactory.getLogger(FhirDocumentApp.class);

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

	public static void main(String[] args) {
        System.getProperties().put( "server.port", 8083 );
		SpringApplication.run(FhirDocumentApp.class, args).close();
	}


    FhirContext ctxSTU3 = FhirContext.forDstu3();



    public static final String SNOMEDCT = "http://snomed.info/sct";


    DateFormat df = new SimpleDateFormat("HHmm_dd_MM_yyyy");

    Composition composition = null;

    private FhirBundleUtil fhirBundleUtil;

    private String consultationId = "2708220";

    CDRInterface cdrInterface = new CDRInterface();

    @Override
	public void run(String... args) throws Exception {

        if (args.length > 0 && args[0].equals("exitcode")) {
            throw new Exception();
        }





        Bundle encounterBundle = buildEncounterDocument(new IdType().setValue(consultationId));

        Date date = new Date();
        String xmlResult = ctxSTU3.newXmlParser().setPrettyPrint(true).encodeResourceToString(encounterBundle);

        Files.write(Paths.get(df.format(date)+"+encounter-"+consultationId+"-document.xml"),xmlResult.getBytes());
        performTransform(xmlResult,df.format(date)+"+"+consultationId+".html","XML/DocumentToHTML.xslt");
        saveToFHIRBinary(df.format(date)+"+"+consultationId+".html", df.format(date)+"+"+consultationId+".json");
        saveToPDF(df.format(date)+"+"+consultationId+".html", df.format(date)+"+"+consultationId+".pdf");
    }


    public Bundle buildEncounterDocument( IdType encounterId) throws Exception {

        FhirDocUtil fhirDoc = new FhirDocUtil(templateEngine);

        fhirBundleUtil = new FhirBundleUtil(Bundle.BundleType.DOCUMENT);
        Bundle compositionBundle = new Bundle();

        // Main resource of a FHIR Bundle is a Composition
        composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        compositionBundle.addEntry().setResource(composition);
        composition.setIdentifier(new Identifier().setSystem("https://tools.ietf.org/html/rfc4122").setValue(composition.getId()));

        // composition.getMeta().addProfile(CareConnectProfile.Composition_1);
        composition.setTitle("Patient Consultation");
        composition.setDate(new Date());
        composition.setStatus(Composition.CompositionStatus.FINAL);

        Organization leedsTH = cdrInterface.getOrganization("M8X3A");
        compositionBundle.addEntry().setResource(leedsTH);

        composition.addAttester()
                .setParty(new Reference("Organization/"+leedsTH.getIdElement().getIdPart()))
                .addMode(Composition.CompositionAttestationMode.OFFICIAL);


        composition.getType().addCoding()
                .setCode("371531000")
                .setDisplay("Report of clinical encounter")
                .setSystem(SNOMEDCT);

        fhirBundleUtil.processBundleResources(compositionBundle);
        fhirBundleUtil.processReferences();


        Bundle encounterBundle = cdrInterface.getEncounterBundleRev(encounterId.getIdPart());
        Encounter encounter = null;
        for(Bundle.BundleEntryComponent entry : encounterBundle.getEntry()) {
            Resource resource =  entry.getResource();
            if (encounter == null && entry.getResource() instanceof Encounter) {
                encounter = (Encounter) entry.getResource();
            }
        }
        String patientId = null;

        if (encounter!=null) {

            patientId = encounter.getSubject().getReferenceElement().getIdPart();
            log.debug(encounter.getSubject().getReferenceElement().getIdPart());


            // This is a synthea patient
            Bundle patientBundle = cdrInterface.getPatientBundle(patientId);

            fhirBundleUtil.processBundleResources(patientBundle);

            if (fhirBundleUtil.getPatient() == null) throw new Exception("404 Patient not found");
            composition.setSubject(new Reference("Patient/"+patientId));
            patientId = fhirBundleUtil.getPatient().getId();


            // date
            if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
                composition.setDate(encounter.getPeriod().getStart());
            }

            // careSetting
            if (encounter.hasClass_() && encounter.getClass_().hasCode()) {
                composition.addExtension()
                        .setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-CareSettingType-1")
                        .setValue(
                                new CodeableConcept().addCoding(
                                        encounter.getClass_()
                                )
                        );
            }

            // author
            if (encounter.hasParticipant()) {
                composition.addAuthor(encounter.getParticipantFirstRep().getIndividual());
            }

            // encounter
            composition.setEncounter(new Reference().setReference(encounter.getId()));

            // custodian
            if (encounter.hasServiceProvider()) {
                composition.setCustodian(encounter.getServiceProvider());
            }
        }


        if (fhirBundleUtil.getPatient() == null) throw new UnprocessableEntityException();
        fhirDoc.generatePatientHtml(fhirBundleUtil.getPatient(),fhirBundleUtil.getFhirDocument());

        // This is not functioning correctly in the viewer.
        //fhirDoc.generateCompositionHtml(composition);

        fhirBundleUtil.processBundleResources(encounterBundle);

        fhirBundleUtil.processReferences();

        composition.addSection(fhirDoc.getEncounterSection(composition, fhirBundleUtil.getFhirDocument()));

        Composition.SectionComponent section = fhirDoc.getConditionSection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        section = fhirDoc.getAttendanceDetailsSection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        section = fhirDoc.getMedicationStatementSection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        /* temp disable 24/3/2022
        section = fhirDoc.getMedicationRequestSection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);
*/
        section = fhirDoc.getAllergySection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        section = fhirDoc.getObservationSection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        section = fhirDoc.getProcedureSection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        section = fhirDoc.getPlansActionsSection(fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        section = fhirDoc.getProvenaceSection(composition, fhirBundleUtil.getFhirDocument());
        if (section.getEntry().size()>0) composition.addSection(section);

        if (fhirBundleUtil.getPatient().hasGeneralPractitioner()) {
            section = fhirDoc.getOrganisation(fhirBundleUtil.getFhirDocument(),
                    fhirBundleUtil.getPatient().getGeneralPractitionerFirstRep().getIdentifier().getValue(),
                    new Coding().setSystem(SNOMEDCT)
                            .setCode("886711000000101")
                            .setDisplay("GP practice"),
                    "GP Practice Details");
            if (section.getEntry().size() > 0) composition.addSection(section);
        }


        return fhirBundleUtil.getFhirDocument();
    }


    private void saveToFHIRBinary(String inputFile, String outputFileName) {
        FileOutputStream os = null;
        File file = new File(inputFile);

        try {
            String processedHtml = org.apache.commons.io.IOUtils.toString(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            final File outputFile = new File(outputFileName);

            os = new FileOutputStream(outputFile);

            org.hl7.fhir.r4.model.Binary binary = new org.hl7.fhir.r4.model.Binary();
            binary.setContentType("text/html");
            binary.setData(processedHtml.getBytes(StandardCharsets.UTF_8));
            FhirContext ctx = FhirContext.forR4();
            os.write(ctx.newJsonParser().encodeResourceToString(binary).getBytes(StandardCharsets.UTF_8));

        }
        catch(Exception ex) {
            System.out.println("ERROR - "+ex.getMessage());
        }
        finally {

            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) { /*ignore*/ }
            }
        }
    }


    private String saveToPDF(String inputFile, String outputFileName) {
        FileOutputStream os = null;
        File file = new File(inputFile);

        try {
            String processedHtml = org.apache.commons.io.IOUtils.toString(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            final File outputFile = new File(outputFileName);

            os = new FileOutputStream(outputFile);

            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(processedHtml);
            renderer.layout();
            renderer.createPDF(os, false);
            renderer.finishPDF();
            return outputFile.getAbsolutePath();
        }
        catch(Exception ex) {
            System.out.println("ERROR - "+ex.getMessage());
        }
        finally {

            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) { /*ignore*/ }
            }
            return null;
        }
    }

    private void performTransform(String xmlInput, String htmlOutput, String styleSheet) {

        // Input xml data file
        ClassLoader classLoader = getContextClassLoader();

        // Input xsl (stylesheet) file
        String xslInput = classLoader.getResource(styleSheet).getFile();

        // Set the property to use xalan processor
        System.setProperty("javax.xml.transform.TransformerFactory",
                "org.apache.xalan.processor.TransformerFactoryImpl");

        // try with resources
        try {
            InputStream xml = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));

            FileOutputStream os = new FileOutputStream(htmlOutput);
            FileInputStream xsl = new FileInputStream(xslInput);

            // Instantiate a transformer factory
            TransformerFactory tFactory = TransformerFactory.newInstance();

            // Use the TransformerFactory to process the stylesheet source and produce a Transformer
            StreamSource styleSource = new StreamSource(xsl);
            Transformer transformer = tFactory.newTransformer(styleSource);

            // Use the transformer and perform the transformation
            StreamSource xmlSource = new StreamSource(xml);
            StreamResult result = new StreamResult(os);
            transformer.transform(xmlSource, result);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}


