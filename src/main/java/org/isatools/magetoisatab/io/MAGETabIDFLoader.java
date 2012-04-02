package org.isatools.magetoisatab.io;

import org.apache.log4j.Logger;
import org.isatools.magetoisatab.io.model.AssayType;
import org.isatools.magetoisatab.io.model.Study;
import org.isatools.magetoisatab.utils.PrintUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by the ISA team
 *
 * @author Philippe Rocca-Serra (proccaserra@gmail.com)
 *         <p/>
 *         Date: 02/03/2011
 *         Time: 18:02
 */

/**
 * A class to convert MAGE-TAB idf file to an ISA-TAB investigation file.
 * The input can be either an ArrayExpress Accession number or the full address of a file on a local file system.
 * The output is an ISA-TAB investigation file.
 */

//TODO: in a number of cases MAGE-TAB encoded data needs relocation and reassignment (typically Chip Antibodies)
//TODO: perform the collapsing the gapped characteristiscs


public class MAGETabIDFLoader {

    private static final Logger log = Logger.getLogger(MAGETabIDFLoader.class.getName());

    public static final Character TAB_DELIM = '\t';

    public String[] sdrfFileNames;

    public String[] aeExpTypes;

    public String[] cmtDesignTypes;


    public List<String> investigationLines;

    //HashMap initialization to define canonical Study Publication block structure
    public Map<Integer, String> IsaPublicationSection = new HashMap<Integer, String>() {
        {
            put(0, "Study PubMed ID");
            put(1, "Study Publication DOI");
            put(2, "Study Publication Author List");
            put(3, "Study Publication Title");
            put(4, "Study Publication Status");
            put(5, "Study Publication Status Term Accession Number");
            put(6, "Study Publication Status Term Source REF");
        }
    };

    public List<String> publicationLines;

    public List<String> studyDesc;
    public List<String> assaylines;
    public List<String> dateLines;


    public Map<Integer, String> IsaOntoSection = new HashMap<Integer, String>() {
        {
            put(0, "Term Source Name");
            put(1, "Term Source File");
            put(2, "Term Source Version");
            put(3, "Term Source Description");
        }
    };


    public List<String> factorLines = new ArrayList<String>() {
        {
            add("Study Factor Name");
            add("Study Factor Type");
            add("Study Factor Type Term Accession Number");
            add("Study Factor Type Term Source REF");
        }
    };

    public List<String> designLines = new ArrayList<String>() {
        {
            add("Study Design Type");
            add("Study Design Type Term Accession Number");
            add("Study Design Type Term Source REF");
        }
    };

    //HashMap initialization to define canonical block structure
    public Map<Integer, String> IsaProtocolSection = new HashMap<Integer, String>() {
        {
            put(0, "Study Protocol Name");
            put(1, "Study Protocol Type");
            put(2, "Study Protocol Type Term Accession Number");
            put(3, "Study Protocol Type Term Source REF");
            put(4, "Study Protocol Description");
            put(5, "Study Protocol URI");
            put(6, "Study Protocol Version");
            put(7, "Study Protocol Parameters Name");
            put(8, "Study Protocol Parameters Name Term Accession Number");
            put(9, "Study Protocol Parameters Name Term Source REF");
            put(10, "Study Protocol Components Name");
            put(11, "Study Protocol Components Type");
            put(12, "Study Protocol Components Type Term Accession Number");
            put(13, "Study Protocol Components Type Term Source REF");
        }
    };


    //HashMap initialization to define canonical block structure
    public Map<Integer, String> IsaContactSection = new HashMap<Integer, String>() {
        {
            put(0, "Study Person Last Name");
            put(1, "Study Person First Name");
            put(2, "Study Person Mid Initials");
            put(3, "Study Person Email");
            put(4, "Study Person Phone");
            put(5, "Study Person Fax");
            put(6, "Study Person Address");
            put(7, "Study Person Affiliation");
            put(8, "Study Person Roles");
            put(9, "Study Person Roles Term Accession Number");
            put(10, "Study Person Roles Term Source REF");
        }
    };

    Map<InvestigationSections, List<String>> investigationSections;


    public MAGETabIDFLoader() {
        investigationSections = new HashMap<InvestigationSections, List<String>>();
    }


    public void loadidfTab(String url, String accnum) throws IOException {

        try {

            String[] sdrfDownloadLocation = {"", "", ""};
            File file = new File(url);

            boolean success = (new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum)).mkdirs();
            if (success) {
                System.out.println("Directory: " + accnum + " created");
            }

            if (file.exists()) {

                Scanner sc = new Scanner(file);

                String line;

                while (sc.hasNextLine()) {

                    if ((line = sc.nextLine()) != null) {

                        if (line.startsWith("Protocol")) {

                            line = line.replaceFirst("Protocol", "Study Protocol");
                            if (!investigationSections.containsKey(InvestigationSections.STUDY_PROTOCOL_SECTION)) {
                                investigationSections.put(InvestigationSections.STUDY_PROTOCOL_SECTION, new ArrayList<String>());
                            }

                            investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).add(line);
                        } else if (line.startsWith("Experiment Desc")) {

                            line = line.replaceFirst("Experiment", "Study");
                            if (studyDesc == null) {
                                studyDesc = new ArrayList<String>();
                            }
                            studyDesc.add(line);
                        } else if (line.startsWith("Person")) {

                            line = line.replaceFirst("Person", "Study Person");
                            if (!investigationSections.containsKey(InvestigationSections.STUDY_CONTACT_SECTION)) {
                                investigationSections.put(InvestigationSections.STUDY_CONTACT_SECTION, new ArrayList<String>());
                            }

                            investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION).add(line);
                        } else if (line.startsWith("PubMed")) {

                            line = line.replaceFirst("PubMed", "Study PubMed");
                            if (publicationLines == null) {
                                publicationLines = new ArrayList<String>();
                            }
                            publicationLines.add(line);
                        }

                        //This is to handle ArrayExpress GEO to MAGE converter propagating PubMed ID to the Publication DOI field
                        else if (line.startsWith("Publication DOI") && line.contains(".")) {

                            line = line.replaceFirst("Publication", "Study Publication DOI");
                            if (publicationLines == null) {
                                publicationLines = new ArrayList<String>();
                            }
                            publicationLines.add(line);
                        } else if ((line.startsWith("Publication")) && !(line.contains("DOI"))) {

                            line = line.replaceFirst("Publication", "Study Publication");
                            if (publicationLines == null) {
                                publicationLines = new ArrayList<String>();
                            }
                            publicationLines.add(line);
                        }

                        //Now Dealing with element from Protocol Section
                        else if (line.startsWith("Experimental Factor Name")) {
                            line = line.toLowerCase();
                            line = line.replaceFirst("experimental factor name", "Study Factor Name");
                            factorLines.set(0, line);
                        } else if (line.startsWith("Experimental Factor Type")) {
                            line = line.toLowerCase();
                            line = line.replaceFirst("experimental factor type", "Study Factor Type");
                            factorLines.set(1, line);
                        } else if (line.endsWith("Factor Term Accession")) {
                            line = line.replaceFirst("Experimental", "Study");
                            factorLines.set(2, line);
                        } else if (line.endsWith("Factor Term Source REF")) {
                            line = line.replaceFirst("Experimental", "Study");
                            factorLines.set(3, line);
                        } else if ((line.contains("Experimental Design")) && (!(line.contains("Experimental Design Term")))) {

                            System.out.println("Experimental Design Tag found at: " + line);

                            line = line.replaceFirst("Experimental Design", "Study Design Type");
                            designLines.set(0, line);
                        }

                        //This bit is used to recover information for setting ISA MT and TT in case no Experimental Design is found
                        else if (line.startsWith("Comment[AEExperimentType")) {

                            System.out.println("Alternative Design Tag found at: " + line);

                            cmtDesignTypes = line.split("\\t");

                            line = line.replace("Comment[AEExperimentType]", "Study Design Type");
                            designLines.set(0, line);

                        } else if (line.startsWith("SDRF File")) {


                            sdrfFileNames = line.split("\\t");

                            System.out.println("number of SDRF files: " + (sdrfFileNames.length - 1));

                            if (sdrfFileNames.length >= 1) {

                                //There is more than one SDRF file listed in this submission, now iterating throw them:");

                                for (int counter = 1; counter < sdrfFileNames.length; counter++) {

                                    String sdrfUrl = "http://www.ebi.ac.uk/arrayexpress/files/" + accnum + "/" + sdrfFileNames[counter];

                                    sdrfDownloadLocation[counter] = DownloadUtils.TMP_DIRECTORY + File.separator + accnum + File.separator + sdrfFileNames[counter];

                                    DownloadUtils.downloadFile(sdrfUrl, sdrfDownloadLocation[counter]);

                                    System.out.println("SDRF found and downloaded: " + sdrfUrl);

                                }

                            }

                            line = line.replaceFirst("SDRF File", "Study Assay File Name");

                            if (assaylines == null) {
                                assaylines = new ArrayList<String>();
                            }
                            assaylines.add(line);
                        } else if (line.startsWith("Investigation")) {
                            line = line.replaceFirst("Investigation", "Study");
                            if (investigationLines == null) {
                                investigationLines = new ArrayList<String>();
                            }
                            investigationLines.add(line);
                        } else if (line.startsWith("Public R")) {
                            line = line.replaceFirst("Public", "Study Public");
                            if (dateLines == null) {
                                dateLines = new ArrayList<String>();
                            }
                            dateLines.add(line);
                        }


                        // looks for information about Ontology and Terminologies used in MAGE-TAB document

                        else if (line.startsWith("Term Source Name")) {

                            String tempLine = "";
                            tempLine = removeDuplicates(line);
                            IsaOntoSection.put(0, tempLine);

                        } else if (line.startsWith("Term Source File")) {

                            String tempLine = "";
                            tempLine = removeDuplicates(line);
                            IsaOntoSection.put(2, tempLine);

                        } else if (line.startsWith("Term Source Version")) {
                            String tempLine = "";
                            tempLine = removeDuplicates(line);
                            IsaOntoSection.put(1, tempLine);

                        } else if (line.startsWith("Term Source Description")) {

                            String tempLine = "";
                            tempLine = removeDuplicates(line);
                            IsaOntoSection.put(3, tempLine);

                        }

                    } else {

                        sc.close();
                    }
                }

                PrintStream invPs = new PrintStream(new File(
                        DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/i_" + accnum + "_investigation.txt"));

                //Outputting the ISA-TAB Ontology Section
                invPs.println("ONTOLOGY SOURCE REFERENCE");

                for (Map.Entry<Integer, String> e : IsaOntoSection.entrySet())
                    invPs.println(e.getValue());

                //Outputing ISA-TAB Investigation Section which is always empty as MAGE-TAB does not support this.
                invPs.println("INVESTIGATION\n" +
                        "Investigation Identifier\n" +
                        "Investigation Title\n" +
                        "Investigation Description\n" +
                        "Investigation Submission Date\n" +
                        "Investigation Public Release Date\n" +
                        "INVESTIGATION PUBLICATIONS\n" +
                        "Investigation PubMed ID\n" +
                        "Investigation Publication DOI\n" +
                        "Investigation Publication Author List\n" +
                        "Investigation Publication Title\n" +
                        "Investigation Publication Status\n" +
                        "Investigation Publication Status Term Accession Number\n" +
                        "Investigation Publication Status Term Source REF\n" +
                        "INVESTIGATION CONTACTS\n" +
                        "Investigation Person Last Name\n" +
                        "Investigation Person First Name\n" +
                        "Investigation Person Mid Initials\n" +
                        "Investigation Person Email\n" +
                        "Investigation Person Phone\n" +
                        "Investigation Person Fax\n" +
                        "Investigation Person Address\n" +
                        "Investigation Person Affiliation\n" +
                        "Investigation Person Roles\n" +
                        "Investigation Person Roles Term Accession Number\n" +
                        "Investigation Person Roles Term Source REF\n" +
                        "\nSTUDY\n" +
                        "Study Identifier" + "\t" + accnum);


                for (String investigationLine : investigationLines) {
                    invPs.println(investigationLine);
                }


                invPs.println("Study Submission Date");

                for (String dateLine : dateLines) {
                    invPs.println(dateLine);
                }

                for (String aStudyDesc : studyDesc) {
                    invPs.println(aStudyDesc);
                }

                invPs.println("Study File Name" + "\t" + "s_" + accnum + "_study_samples.txt");

                invPs.println("STUDY DESIGN DESCRIPTORS");

                for (int i = 0; i < designLines.size(); i++) {
                    //System.out.println(designLines.get(i));
                    invPs.println(designLines.get(i));
                }


                invPs.println("STUDY PUBLICATIONS");

                if (publicationLines.size() > 0) {
                    for (String publicationLine : publicationLines) {

                        if (publicationLine.contains("PubMed")) {
                            IsaPublicationSection.put(0, publicationLine);
                        }
                        if (publicationLine.contains("DOI")) {
                            IsaPublicationSection.put(1, publicationLine);
                        }
                        if (publicationLine.contains("List")) {
                            IsaPublicationSection.put(2, publicationLine);
                        }
                        if (publicationLine.contains("Title")) {
                            IsaPublicationSection.put(3, publicationLine);
                        }
                        if ((publicationLine.contains("Status")) && !(publicationLine.contains("Status Term"))) {
                            IsaPublicationSection.put(4, publicationLine);
                        }
                        if (publicationLine.contains("Status Term Accession")) {
                            IsaPublicationSection.put(5, publicationLine);
                        }
                        if (publicationLine.contains("Status Term Source")) {
                            IsaPublicationSection.put(6, publicationLine);
                        }
                    }
                }

                //we now output the Publication Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaPublicationSection.entrySet())
                    invPs.println(e.getValue());


                // Now Creating the Factor Section
                invPs.println("STUDY FACTORS");

                for (String factorLine : factorLines) {
                    invPs.println(factorLine);
                }


                //Now creating the Assay Section:
                invPs.println("STUDY ASSAYS");

                // We are now trying to get the Measurement and Technology Type from MAGE annotation Experimental Design Type

                List<AssayType> assayTTMT = getMeasurementAndTech(designLines.get(0));

                // If this fails, we are falling back on checking MAGE-TAB Comment[AEExperimentType] line
                String measurementTypes = "Study Assay Measurement Type";
                String technologyTypes = "Study Assay Technology Type";

                for (int i = 0; i < assayTTMT.size(); i++) {
                    measurementTypes = measurementTypes + "\t" + assayTTMT.get(i).getMeasurement();
                    technologyTypes = technologyTypes + "\t" + assayTTMT.get(i).getTechnology();
                }

                invPs.println(measurementTypes);
                invPs.println("Study Assay Measurement Type Term Accession Number\n" +
                        "Study Assay Measurement Type Term Source REF");


                invPs.println(technologyTypes);
                invPs.println("Study Assay Technology Type Term Accession Number\n" +
                        "Study Assay Technology Type Term Source REF\n" +
                        "Study Assay Technology Platform");


                String assayfilenames = "Study Assay File Name";


                //we now create as many assay spreadsheet as needed:

                //case1: there is only SDRF and we rely on the information found under Comment[AEexperimentTypes]
                //NOTE: caveat: AE is inconsistent and encode various measurement types under the same spreadsheet for sequencing applications
                if ((assayTTMT.size() > 0) && ((sdrfFileNames.length - 1) == 1)) {

                    for (int i = 0; i < assayTTMT.size(); i++) {       //we start at 1 as the first element of the array is the header "

                        assayfilenames = assayfilenames + "\ta_" + accnum + "_" + assayTTMT.get(i).getShortcut() + "_assay.txt";

                        System.out.println("CASE1: " + assayTTMT.get(i).getShortcut());
                    }

                    System.out.println("CASE1: " + assayfilenames);
                }

                //case2: there are more than 1 SDRF and we rely on the information found under Comment[AEexperimentTypes]
                else if ((sdrfFileNames.length > 1) && (cmtDesignTypes.length - 1 > 1) && (sdrfFileNames.length == cmtDesignTypes.length)) {

                    for (int i = 1; i < cmtDesignTypes.length; i++) { //we start at 1 as the first element of the array is the header "
                        // System.out.println("design-before: "+cmtDesignTypes[i]);

                        if (cmtDesignTypes[i].toLowerCase().contains("chip-seq")) {

                            for (AssayType anAssayTTMT : assayTTMT) {
                                if ((anAssayTTMT.getMeasurement().equalsIgnoreCase("protein-DNA binding site identification")) &&
                                        (anAssayTTMT.getTechnology().equalsIgnoreCase("nucleotide sequencing")))

                                {
                                    cmtDesignTypes[i] = cmtDesignTypes[i].replaceAll("ChIP-seq", "ChIP-Seq");
                                    anAssayTTMT.setFile(cmtDesignTypes[i]);
                                }
                            }
                        }

                        if (cmtDesignTypes[i].toLowerCase().contains("transcription profiling by array")) {
                            for (AssayType anAssayTTMT : assayTTMT) {

                                if ((anAssayTTMT.getMeasurement().equalsIgnoreCase("transcription profiling")) &&
                                        (anAssayTTMT.getTechnology().equalsIgnoreCase("DNA microarray"))) {

                                    // System.out.println("design-after-1st: "+cmtDesignTypes[i]);
                                    cmtDesignTypes[i] = cmtDesignTypes[i].replaceAll("transcription profiling by array", "GeneChip");
                                    anAssayTTMT.setFile(cmtDesignTypes[i]);
                                }
                            }
                            // System.out.println("design-after-2nd: "+cmtDesignTypes[i]);
                        }

                    }


                    for (int i = 0; i < assayTTMT.size(); i++) {
                        assayfilenames = assayfilenames + "\ta_" + accnum + "_" + assayTTMT.get(i).getFile().replaceAll("\\s", "_") + "_assay.txt";
                    }
                    //need to reorder to match MT and TT declaration:


                    System.out.println("CASE2: " + assayfilenames);
                }

                //now we can output that IDF row containing all

                invPs.println(assayfilenames);

                //Now creating the Protocol section
                invPs.println("STUDY PROTOCOLS");


                if (investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).size() > 0) {

                    for (String protocolLine : investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION)) {

                        if (protocolLine.contains("Name")) {
                            IsaProtocolSection.put(0, protocolLine);
                        }

                        if (protocolLine.contains("Type")) {
                            IsaProtocolSection.put(1, protocolLine);
                        }

                        if (protocolLine.contains("Accession")) {
                            String tempAcc = protocolLine.replaceAll("Term Accession", "Type Term Accession");
                            IsaProtocolSection.put(2, tempAcc);
                        }

                        if (protocolLine.contains("Term Source")) {
                            String tempSource = protocolLine.replaceAll("Term Source", "Type Term Source");
                            IsaProtocolSection.put(3, tempSource);
                        }

                        if (protocolLine.contains("Description")) {
                            IsaProtocolSection.put(4, protocolLine);
                        }

                        if (protocolLine.endsWith("Parameters")) {

                            String tempParam = protocolLine.replaceAll("Parameters", "Parameters Name");
                            IsaProtocolSection.put(5, tempParam);
                        }

                        if ((protocolLine.contains("Software")) || (protocolLine.contains("Hardware"))) {

                            String tempComponent = protocolLine.replaceAll("Software", "Components Name");
                            tempComponent = tempComponent.replaceAll("Hardware", "Components Name");
                            IsaProtocolSection.put(10, tempComponent);
                        }
                    }
                }

                //we now output the Protocol Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaProtocolSection.entrySet())
                    invPs.println(e.getValue());


                // Let's now deal with the Contact Information Section
                invPs.println("STUDY CONTACTS");

                if (investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION).size() > 0) {

                    for (String contactLine : investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION)) {

                        if (contactLine.contains("Last")) {
                            IsaContactSection.put(0, contactLine);
                        }
                        if (contactLine.contains("First")) {
                            IsaContactSection.put(1, contactLine);
                        }
                        if (contactLine.contains("Mid")) {
                            IsaContactSection.put(2, contactLine);
                        }
                        if (contactLine.contains("Email")) {
                            IsaContactSection.put(3, contactLine);
                        }
                        if (contactLine.contains("Phone")) {
                            IsaContactSection.put(4, contactLine);
                        }
                        if (contactLine.contains("Fax")) {
                            IsaContactSection.put(5, contactLine);
                        }
                        if (contactLine.contains("Address")) {
                            IsaContactSection.put(6, contactLine);
                        }
                        if (contactLine.contains("Affiliation")) {
                            IsaContactSection.put(7, contactLine);
                        }
                        if ((contactLine.contains("Roles") && !(contactLine.contains("Roles Term")))) {

                            IsaContactSection.put(8, contactLine);
                        }
//                        if (contactLine.contains("Roles Term Accession")) {
//
//                            IsaContactSection.put(9, contactLine);
//                        }
////
//                       //
//                      if (contactLine.contains("Roles Term Source")) {
//
//                            IsaContactSection.put(10, contactLine);
//                        }
                    }
                }

                //we now output the Contact Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaContactSection.entrySet())
                    invPs.println(e.getValue());


                //for each sdrf found in the IDF, perform processing
                if ((sdrfFileNames.length - 1 == 1)) {      //the regular situation, one SDRF per MAGE-TAB file

                    MAGETabSDRFLoader sdrfloader = new MAGETabSDRFLoader();

                    Study study = sdrfloader.loadsdrfTab(sdrfDownloadLocation[1], accnum);

                    PrintUtils pu = new PrintUtils();

                    PrintStream ps = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/s_" + accnum + "_" + "study_samples.txt"));

                    pu.printStudySamplesAndAssays(ps, study, accnum);

                    //closing file handle
                    ps.flush();
                    ps.close();

                } else {   //in case there are more than 1 SDRF file declared

                    PrintUtils pu = new PrintUtils();

                    PrintStream ps = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/s_" + accnum + "_" + "study_samples.txt"));

                    List<LinkedHashMap<String, ArrayList<String>>> Studies = new ArrayList<LinkedHashMap<String, ArrayList<String>>>();

                    //we start at 1 as the first element of the sdrfFilenames array corresponds to the ISA Study File Name tag
                    for (int counter = 1; counter < sdrfFileNames.length; counter++) {

                        System.out.println("SDRF number " + counter + " is:" + sdrfDownloadLocation[counter]);

                        MAGETabSDRFLoader sdrfloader = new MAGETabSDRFLoader();

                        Study study = sdrfloader.loadsdrfTab(sdrfDownloadLocation[counter], accnum);

                        LinkedHashMap<String, ArrayList<String>> table = new LinkedHashMap<String, ArrayList<String>>();

                        //for every
                        for (int i = 0; i < study.getStudySampleLevelInformation().get(0).length; i++) {

                            String key = study.getStudySampleLevelInformation().get(0)[i];

                            ArrayList<String> values = new ArrayList<String>();

                            for (int k = 1; k < study.getStudySampleLevelInformation().size(); k++) {

                                String value = study.getStudySampleLevelInformation().get(k)[i].toString();

                                if (value != null) {
                                    values.add(value);
                                }

                            }

                            table.put(key, values);

                        }

                        Studies.add(table);

                        //TODO: for each study object, create a new Pair of ArrayList of String[], one holding the various study-sample tables
                        //which will require processing for fusing them
                        //accumulate the assays if needed.
                        //TODO: when more than one SDRF is found, additional Processing maybe needed to support creation of an investigation

                        pu.printStudySamplesAndAssays(ps, study, accnum);
                    }


                    mergeTables(Studies);

                    //this set's keys are the final header of the merged study sample file
                    Set<String> tableKeyset = mergeTables(Studies).keySet();
                    String finalStudyTableHeader = "";
                    Iterator iterator = tableKeyset.iterator();

                    //we now splice the header together by concatenating the key
                    while (iterator.hasNext()) {
                        finalStudyTableHeader = finalStudyTableHeader + iterator.next().toString() + "\t";
                    }
                    //we print the header
                    ps.println(finalStudyTableHeader);

                    //we now need to get the total number of records. This corresponds to the number of elements in the arrays associated to the key "Sample Name"

                    int numberOfSampleRecords;

                    ArrayList<String> guestList = new ArrayList<String>();
                    guestList = (ArrayList<String>) mergeTables(Studies).get("Sample Name");

                    numberOfSampleRecords = guestList.size();

                    Set<String> finalStudyTable = new HashSet<String>();

                    for (int i = 0; i < numberOfSampleRecords; i++) {

                        String studyRecord = "";

                        for (Object key : mergeTables(Studies).keySet()) {

                            //obtain the list associated to that given key
                            ArrayList<String> correspondingList = new ArrayList<String>();
                            correspondingList = (ArrayList<String>) mergeTables(Studies).get(key);

                            // now obtain the ith element of that associated list
                            if (i < correspondingList.size()) {

                                studyRecord += correspondingList.get(i) + "\t";
                            } else {
                                studyRecord += "" + "\t";
                            }
                        }

                        finalStudyTable.add(studyRecord);
                    }

                    //Here we print the new records to the final study sample file
                    Iterator itr = finalStudyTable.iterator();
                    while (itr.hasNext()) {
                        ps.println(itr.next());
                    }

                    //closing file handle
                    ps.flush();
                    ps.close();
                }

            } else {
                System.out.println("ERROR: File not found");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private LinkedHashMap mergeTables(List<LinkedHashMap<String, ArrayList<String>>> studies) {

        int i = 0;

        LinkedHashSet<String> allKeys = new LinkedHashSet<String>();

        LinkedHashMap<String, ArrayList<String>> resultMap = new LinkedHashMap<String, ArrayList<String>>();

        allKeys.addAll(studies.get(i).keySet());
        allKeys.addAll(studies.get(i + 1).keySet());

        for (String k : allKeys) {

            ArrayList<String> i1 = studies.get(i).containsKey(k) ? studies.get(i).get(k) : null;
            ArrayList<String> i2 = studies.get(i + 1).containsKey(k) ? studies.get(i + 1).get(k) : null;

            ArrayList<String> newList = new ArrayList<String>(i1);

            if (i2 != null) {
                newList.addAll(i2);
            }

            resultMap.put(k, newList);

        }

        return resultMap;
    }


    /**
     * A method to remove duplicate entries in Ontology Section
     */
    private String removeDuplicates(String ontoline) {

        String newLine = "";

        String[] stringArray = ontoline.split("\\t");

        ArrayList<String> stringArrayList = new ArrayList<String>(Arrays.asList(stringArray));

        Set<String> set = new LinkedHashSet<String>(stringArrayList);

        Iterator iter = set.iterator();

        while (iter.hasNext()) {

            newLine = newLine + iter.next().toString() + "\t";

        }
        return newLine;

    }


    /**
     * A method that uses MAGE-TAB Experiment Design information to deduce ISA Measurement and Technology Types.
     *
     * @param line
     * @return A Pair of Strings containing the Measurement and Technology Types to be output
     *         TODO: rely on an xml configuration file to instead of hard coded values -> easier to maintain in case of changes in ArrayExpress terminology
     */
    private List<AssayType> getMeasurementAndTech(String line) {

        // Line can contain multiple technologies. We must output them all as AssayTypes.

        List<AssayType> assayTypes = new ArrayList<AssayType>();

        if (line.matches("(?i).*ChIP-Chip.*")) {
            assayTypes.add(new AssayType("protein-DNA binding site identification", "DNA microarray", "ChIP-Chip"));
        }

        if ( (line.matches("(?i).*RNA-seq.*"))  || (line.matches("(?i).*RNA-Seq.*")) || (line.matches("(?i).*transcription profiling by high throughput sequencing.*"))){
            assayTypes.add(new AssayType("transcription profiling", "nucleotide sequencing", "RNA-Seq"));
        }

        if (line.matches(".*transcription profiling by array.*")) {
            assayTypes.add(new AssayType("transcription profiling", "DNA microarray", "GeneChip"));
        }
        if (line.matches("(?i).*methylation profiling by array.*")) {
            assayTypes.add(new AssayType("DNA methylation profiling", "DNA microarray", "Me-Chip"));

        }
        if (line.matches("(?i).*comparative genomic hybridization by array.*")) {
            assayTypes.add(new AssayType("comparative genomic hybridization", "DNA microarray", "CGH-Chip"));
        }
        if (line.matches(".*genotyping by array.*")) {
            assayTypes.add(new AssayType("SNP analysis", "DNA microarray", "SNPChip"));
        }
//        if (line.matches("(?i).*transcription profiling by high throughput sequencing.*")) {
//            assayTypes.add(new AssayType("transcription profiling", "nucleotide sequencing", "RNA-Seq"));
//        }
        if ( (line.matches("(?i).*ChIP-Seq.*")) || (line.matches("(?i).*chip-seq.*"))  ) {
            assayTypes.add(new AssayType("protein-DNA binding site identification", "nucleotide sequencing", "ChIP-Seq"));
        }
//        if (line.matches("(?i).*chip-seq.*")) {
//            assayTypes.add(new AssayType("protein-DNA binding site identification", "nucleotide sequencing", "ChIP-Seq"));
//        }
//        if (line.matches("(?i).*RNA-Seq.*")) {
//            assayTypes.add(new AssayType("transcription profiling", "nucleotide sequencing", "RNA-Seq"));
//        }

        return assayTypes;

    }


}