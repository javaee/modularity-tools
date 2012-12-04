/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.enterprise.tools.visualizer.hk2;


import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import oracle.xml.differ.*;


/**
 * reads a wires XML file generated by the dependency-verifier and outputs a dot
 * file (Use <a href="http://www.graphviz.org/">GraphViz</a> to create images
 * from the dot file) that helps visualizing the wiring dependencies between
 * packages. The tool also supports viewing the dependencies of only a subset of
 * the packages.
 * 
 * @author Sivakumar Thyagarajan
 */
public class DotGenerator {
	private final static boolean DEBUG = false;

	PrintStream wireOut = null;
	GeneratorOptions options = null;

	/**
	 * An internal class holding all the command line options
	 * supported by this DotGenerator
	 * 
	 * @author Sivakumar Thyagarajan
	 */
	static class GeneratorOptions {
		// Accept XML files generated by HK2 dependency-verifier
		@Option(name = "-i", usage = "Input Wires XML file", required = true)
		public String input = "wires.xml";

        @Option(name = "-b", usage = "Input bundles XML file")
		public String binput = "bundles.xml";

		@Option(name = "-o", usage = "Output DOT file")
		public String output;

		@Option(name = "-m", usage = "Show only packages that contains the specified substring")
		public String match = "";// By default, match all.

                // From Vijay. Extend with a new option to specify bundle name.

                @Option(name = "-n", usage = "Show only the specified bundle and its package wiring with other bundles")
                public String bundlename = "";// By default, match all.

                @Option(name = "-drawtype", usage = "imports/exports/both")
                public String drawtype = "";// By default, imports.

                @Option(name = "-XML", usage = "true/false")
                public String xml = "";// By default, true.

                @Option(name = "-XMLDiff", usage = "true/false")
                public String xmldiff = "";// By default, true.

                @Option(name = "-f1", usage = "First XML file")
                public String file_1 = "";// By default, true.

                @Option(name = "-f2", usage = "Second XML file")
                public String file_2 = "";// By default, true.

                @Option(name = "-outdir", usage = "output dir for diff file")
                public String out_dir = "";// By default, true.

                @Option(name = "-filename", usage = "diff file name")
                public String diff_file = "";// By default, true.


                @Option(name = "-f", usage = "File containing module names for which the wiring needs to be visualized")
                public String filename = "";// By default, match all.

                @Option(name = "-drawgroup", usage = "set/all; set for wiring details only among modules in file and all for all modules")
                public String group = "";// By default, set.


		// receives other command line parameters than options
		@Argument
		public List<String> arguments = new ArrayList<String>();
	}
	
	/**
	 * A simple class representing all the information about a
	 * package that can be derived from the wires.xml
	 * 
	 * @author Sivakumar Thyagarajan
	 */
	class PackageInfo {
		String packageName, exportedBy = null;
		String[] importedBy = null;

		public PackageInfo(String packageName, String exportedBy,
				String[] importedBy) {
			this.packageName = packageName;
			this.exportedBy = exportedBy;
			this.importedBy = importedBy;
		}
	}

    class BundleInfo {

        String bundleName, exportUsed, exportUnused, totalImports, totalExports = null;

        int level = 0;
    //    List<String> ExportModules = new ArrayList<String>();
    //    List<String> ImportModules = new ArrayList<String>();

        Set<String> ExportModules = new HashSet<String>();
        Set<String> ImportModules = new HashSet<String>();


        public BundleInfo(String bundleName, String exportUsed, String exportUnused, String totalImports, String totalExports,Set<String> ExportModules,Set<String> ImportModules ) {

            this.bundleName = bundleName;
            this.exportUsed = exportUsed;
            this.exportUnused = exportUnused;
            this.totalImports = totalImports;
            this.totalExports = totalExports;
            this.ExportModules = ExportModules;
            this.ImportModules = ImportModules;
        }


        public int getLevel(){
            return this.level;

        }

        public void setLevel(int level){
            this.level=level;
        }

        public String getBundleName(){
            return this.bundleName;
        }

    }
	
	public DotGenerator(GeneratorOptions go) throws Exception {
		this.options = go;
		initXML();
        if(this.options.xml.equalsIgnoreCase("true")){
        initBundleXML();
        generatestatXML();
        return;}

        if(this.options.xmldiff.equalsIgnoreCase("true")){

            if ((this.options.file_1.isEmpty() && this.options.file_2.isEmpty() && this.options.out_dir.isEmpty() && this.options.diff_file.isEmpty())){

             System.err.println("Please provide all options for XML diff namely f1,f2,outdir and filename");
               return;

            }

        XMLDiffer();
        return;}


        wireOut = new PrintStream(new FileOutputStream(this.options.output));
        generate();

	}

    /*
    private void writeFile (Set<String> s) throws IOException{

        try{
        FileWriter fstream = new FileWriter("ModuleNames.txt");
        BufferedWriter out = new BufferedWriter(fstream);
        Iterator its = s.iterator();
        while (its.hasNext()) {
        out.write(its.next().toString()+"\n");
        }
        out.close();
        }catch (Exception e){//Catch exception if any
        System.err.println("Error: " + e.getMessage());
        }

    }
     */


   private void XMLDiffer() throws Exception {

       String file1, file2, out, diff = null;

       file1 = this.options.file_1;
       file2 = this.options.file_2;
       out = this.options.out_dir;
       diff = this.options.diff_file;


       try{
        XMLDiff xmldiff = new XMLDiff();
        xmldiff.setUrl1(file1);
        xmldiff.setUrl2(file2);
        xmldiff.diff();
        xmldiff.generateXSLFile(out+"/"+diff);
       }
       catch (Exception e)   {
         System.err.println("Error: " + e.getMessage()
					+ e.getLocalizedMessage());
    }

    }

    private void generatestatXML()  throws Exception {

        PackageInfo[] refpkgInfo = getPackageXML();
        BundleInfo[] bndlInfo = getBundleXML();
        BundleInfo[] copybndlInfo = bndlInfo;

        //PackageInfo[] refpkgInfo =  pkgInfo;
        Set<String> s = new HashSet<String>();

        for (PackageInfo pkgs : refpkgInfo) {
             // if (!s.add(pkgs.exportedBy)){
              //    continue;
              //}
            if (!pkgs.exportedBy.isEmpty() && !pkgs.exportedBy.matches(".*\\s+.*"))
            {
            //boolean isspace = pkgs.exportedBy.matches("^\\\\s*$");
            //System.out.println(pkgs.exportedBy);
            s.add(pkgs.exportedBy);

            }
        }

      //  writeFile(s);

		try{

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("ModuleStats");
		doc.appendChild(rootElement);

        Iterator it = s.iterator();

        while (it.hasNext()) {


        // Module elements
		Element module = doc.createElement("Module");
		rootElement.appendChild(module);

        String ExportModule = it.next().toString();
       // System.out.println(ExportModule);
        //System.out.println(s.size());
        int imports = 0;
        int exports = 0;

       // For bundles.xml file handling

         String used = null;
         String unused = null;
         String texports = null;
         String timports = null;
         String classes = "";
         String stability = "";
         String instability = "";
         String layer = "";

        List<BundleInfo> bndList = new ArrayList<BundleInfo>(Arrays.asList(copybndlInfo));
        BundleInfo my_bundle = getBundle(ExportModule,bndList);
        layer = ((Integer)my_bundle.getLevel()).toString();

        for (BundleInfo refbdl : bndlInfo) {
            if(refbdl.bundleName.contains(ExportModule)){
                used = refbdl.exportUsed;
                unused = refbdl.exportUnused;
                texports = refbdl.totalExports;
                timports = refbdl.totalImports;

            }

        }

        for (PackageInfo refpkg : refpkgInfo) {
            if(refpkg.exportedBy.contains(ExportModule)) {

                for (int e=0; e<refpkg.importedBy.length; e++) {
                    if (!refpkg.importedBy[e].contains(ExportModule)){
                    imports++;
                    }
                }
            }

        for (int d=0; d<refpkg.importedBy.length; d++) {
            if (refpkg.importedBy[d].contains(ExportModule) && !refpkg.exportedBy.contains(ExportModule)) {
                exports++;
            }
        }

        }

        Integer i = imports;
        Integer e = exports;

        // Modulename elements
		Element modulename = doc.createElement("name");
		modulename.appendChild(doc.createTextNode(ExportModule));
		module.appendChild(modulename);

        //Import elements
        Element imp = doc.createElement("import");
		imp.appendChild(doc.createTextNode(i.toString()));
		module.appendChild(imp);

        //Export elements
        Element export = doc.createElement("export");
		export.appendChild(doc.createTextNode(e.toString()));
		module.appendChild(export);

        //UsedExport elements
        Element uexport = doc.createElement("used-export");
		uexport.appendChild(doc.createTextNode(used));
		module.appendChild(uexport);

        //UnusedExport elements
        Element unexport = doc.createElement("unused-export");
		unexport.appendChild(doc.createTextNode(unused));
		module.appendChild(unexport);

        //TotalExport elements
        Element texport = doc.createElement("total-export");
		texport.appendChild(doc.createTextNode(texports));
		module.appendChild(texport);

        //TotalImport elements
        Element timport = doc.createElement("total-import");
		timport.appendChild(doc.createTextNode(timports));
		module.appendChild(timport);

        //Total no of classes
        Element no_classes = doc.createElement("classes");
		no_classes.appendChild(doc.createTextNode(classes));
		module.appendChild(no_classes);

        //Stability metrics elements
        Element no_stability = doc.createElement("stability");
		no_stability.appendChild(doc.createTextNode(stability));
		module.appendChild(no_stability);

        //Unstability  metrics elements
        Element no_instability = doc.createElement("instability");
		no_instability.appendChild(doc.createTextNode(instability));
		module.appendChild(no_instability);

        //Layer no elements
        Element no_layer = doc.createElement("layer");
		no_layer.appendChild(doc.createTextNode(layer));
		module.appendChild(no_layer);


        }

        // write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty
        (OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
        // we want to pretty format the XML output
        // transformer.setOutputProperty
        //("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);


          Node pi = doc.createProcessingInstruction
         ("xml-stylesheet", "type=\"text/xsl\" href=\"ModuleStyle.xsl\"");
          doc.insertBefore(pi, rootElement);



        StreamResult result = new StreamResult(new File("ModuleStats.xml"));

		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);

		transformer.transform(source, result);

		System.out.println("File saved!");

         // Transformation to HTML


        TransformerFactory tFactory = TransformerFactory.newInstance();
        //Transformer Transformer = tFactory.newTransformer(new StreamSource("Bundles.xsl"));
        Transformer Transformer = tFactory.newTransformer(new StreamSource(new File("Bundles.xsl")));
        Transformer.transform( new StreamSource(new File("bundles.xml")),new StreamResult(new FileOutputStream("Bundles.html")));



           // TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer = tFactory.newTransformer(new StreamSource(new File("wires.xsl")));
            Transformer.transform( new StreamSource(new File("wires.xml")),new StreamResult(new FileOutputStream("wires.html")));

       }
      catch (ParserConfigurationException pce) {
		pce.printStackTrace();
	  } catch (TransformerException tfe) {
		tfe.printStackTrace();
	  }


    }

	private Document doc = null;
    private Document bdoc = null;


	private void initXML() throws Exception {
		File file = new File(this.options.input);
		debug("file " + file);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		doc = db.parse(file);
		doc.getDocumentElement().normalize();
	}

    private void initBundleXML() throws Exception {
		File file = new File(this.options.binput);
		debug("file " + file);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		bdoc = db.parse(file);
		bdoc.getDocumentElement().normalize();
    }


	private PackageInfo[] getPackageXML() throws IOException {
		NodeList pkgLst = doc.getElementsByTagName("Package");
		debug("Package count:" + pkgLst.getLength());
		List<PackageInfo> pkgInfos = new ArrayList<PackageInfo>();
		for (int i = 0; i < pkgLst.getLength(); i++) {
			Node pkgNode = pkgLst.item(i);
			if (pkgNode.getNodeType() == Node.ELEMENT_NODE) {
				Element packageElement = (Element) pkgNode;
				// exports
				NodeList ExportersList = packageElement
						.getElementsByTagName("Exporters");
				Element exporterElt = (Element) ExportersList.item(0);
				String exporter = ((Node) exporterElt.getChildNodes().item(0))
						.getNodeValue().trim();
				debug("Exporter : " + exporter);

				// importers
				NodeList importersList = packageElement
						.getElementsByTagName("Importers");
				Element importerElt = (Element) importersList.item(0);
				String importers = ((Node) importerElt.getChildNodes().item(0))
						.getNodeValue().trim();
				debug("Importers : " + importers);

				// Get package name and return PackageInfos
				String pkgName = packageElement.getAttribute("name").trim();
				debug("Package Name : " + pkgName);
				PackageInfo pkgInfo = new PackageInfo(pkgName, exporter,
						split(importers));
				pkgInfos.add(pkgInfo);
			}
		}
		return pkgInfos.toArray(new PackageInfo[] {});
	}


    private BundleInfo[] getBundleXML() throws IOException {

        NodeList bdLst = bdoc.getElementsByTagName("Bundle");
        debug("Bundles count:" + bdLst.getLength());
        List<BundleInfo> bdInfos = new ArrayList<BundleInfo>();
        PackageInfo[] Info = getPackageXML();

        for (int i = 0; i < bdLst.getLength(); i++) {
            Node bdNode = bdLst.item(i);
            if (bdNode.getNodeType() == Node.ELEMENT_NODE) {
                Element packageElement = (Element) bdNode;

                // Get Bundle  name and attributes and return bdInfos

                String bName = packageElement.getAttribute("name").trim();
                String uexport = packageElement.getAttribute("used").trim();
                String unexport = packageElement.getAttribute("unused").trim();
                String timports = packageElement.getAttribute("total-imports").trim();
                String texports = packageElement.getAttribute("total-exports").trim();

                debug("Bundle Name : " + bName);


                ///////////////////////////////////////////////////////////////

                if (bName == null || bName == "")
                    System.out.println("Null Names exist");

                PackageInfo[] pkInfo = Info;


                // For each bundle name find list of export bundles and import bundles from wires.xml info in pkinfo's.
                //     List<String> ExportModules = new ArrayList<String>();
                //     List<String> ImportModules = new ArrayList<String>();

                Set<String> ExportModules = new HashSet<String>();
                Set<String> ImportModules = new HashSet<String>();

                for (PackageInfo pkg : pkInfo) {
                    if (pkg.exportedBy != "" || pkg.exportedBy != null) {

                        //  System.out.println("Pkg Name:Bname= " + pkg.exportedBy + bName);
                        if (bName.equals(pkg.exportedBy)) {
                            List<String> group = Arrays.asList(pkg.importedBy);
                            ExportModules.addAll(group);

                        }

                        PackageInfo[] copypkInfo = Info;
                        for (PackageInfo cpkg : copypkInfo) {

                            if (pkg.importedBy != null || pkg.importedBy.length != 0) {
                                List<String> impgroup = Arrays.asList(cpkg.importedBy);
                                if (impgroup.contains(bName)) {
                                    ImportModules.add(cpkg.exportedBy);
                                }
                            }

                        }

                    }
                }

                if (ExportModules.contains(bName))
                    ExportModules.remove(bName);

                if (ImportModules.contains(bName))
                    ImportModules.remove(bName);

                //////////////////////////////////////////////////////////////////////
                //   System.out.println("Bundle Name: " + bName);
                //   System.out.println("Export List: " + ExportModules.toString());
                //   System.out.println("Import List: " + ImportModules.toString());

                BundleInfo bInfo = new BundleInfo(bName, uexport, unexport, timports, texports, ExportModules, ImportModules);
                bdInfos.add(bInfo);
            }
        }

        //////////////////////////////////////////////
        List<BundleInfo> copybdInfos = new ArrayList<BundleInfo>();
        copybdInfos = bdInfos;
        int j = 0;

        for (BundleInfo bdl : copybdInfos) {

            //  System.out.println("Iteration " + ++j);

            String bndl_name = bdl.getBundleName();
            BundleInfo MyBndl = getBundle(bndl_name, bdInfos);
            //  System.out.println("BundleName: " + bndl_name);

            int level = MyBndl.getLevel();
            int max_level = 0;
            Iterator iter = bdl.ImportModules.iterator();

            if (!bdl.ImportModules.isEmpty()) {

                while (iter.hasNext()) {
                    String Iname = (String) iter.next();
                    //  System.out.println("Import BName " + Iname);
                    BundleInfo ImpBndl = getBundle(Iname, bdInfos);
                    if (ImpBndl == null)
                        continue;
                    int imp_level = ImpBndl.getLevel();
                    //  System.out.println("Import BLevel  " + imp_level);
                    //  System.out.println("Max BLevel " + max_level);
                    if (imp_level > max_level)
                        max_level = imp_level;

                }

                if (level <= max_level)
                    level = max_level + 1;

            }

            MyBndl.setLevel(level);
            //System.out.println("Set BLevel " + level);
            iter = bdl.ExportModules.iterator();

            int elevel = level + 1;

            while (iter.hasNext()) {
                BundleInfo ExpBndl = getBundle((String) iter.next(), bdInfos);
                if (ExpBndl == null)
                    continue;
                if (ExpBndl.getLevel() <= level) {
                    ExpBndl.setLevel(elevel);
                }
                //System.out.println("Export Level BName:level " + ExpBndl.bundleName + ExpBndl.getLevel() );
            }

        }

        for (BundleInfo bmdl : bdInfos) {

            System.out.println("Bundle Name: " + bmdl.bundleName + " Level No: " + bmdl.getLevel());
        }


        //////////////////////////////////////////////////


        return bdInfos.toArray(new BundleInfo[]{});
    }


    private BundleInfo getBundle(String name, List<BundleInfo> BundleList) {
        String b_name = name;
        List<BundleInfo> bList = BundleList;

        BundleInfo sbundle = null;

        for (BundleInfo bdl : bList) {
            //   System.out.println("In LOOP"+ bdl.bundleName);
            if (b_name.equalsIgnoreCase(bdl.bundleName)) {
                return bdl;
            }
        }

        //System.out.println("Not Found Bundle: " + b_name);

        return sbundle;

    }


    private ArrayList<String> getbundleNames(String file) throws IOException {
        ArrayList<String> modules = new ArrayList<String>();
        try {
            String fileName = file;
            FileInputStream fstream = new FileInputStream(fileName);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {

                modules.add(strLine);

            }
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage()
                    + e.getLocalizedMessage());
        }
        return modules;

    }


    private void generate() throws Exception {
            generateDotStart();
            PackageInfo[] pkgInfos = getPackageXML();
            String drawoption = this.options.drawtype.trim();
            if (drawoption.isEmpty()){drawoption="imports";}
            if (!drawoption.equalsIgnoreCase("exports") && !drawoption.equalsIgnoreCase("imports") && !drawoption.equalsIgnoreCase("both")){
               System.err.println("Not a valid drawtype. Only allowed options are imports, exports & both");
               return;
            }

            String drawGroup = this.options.group.trim();
            if (drawGroup.isEmpty()){drawGroup="set";}
             if (!drawGroup.equalsIgnoreCase("set") && !drawGroup.equalsIgnoreCase("all")){
               System.err.println("Not a valid drawGroup. Only allowed options are set & all");
               return;
            }

            for (PackageInfo pkgInfo : pkgInfos) {
                // Match if needed
                String matchString = this.options.match.trim();
                boolean matchNeeded = !matchString.isEmpty();
                    // New Snippet to support -n & -f option
                            String bundleString = this.options.bundlename.trim();
                            boolean bundleOnly = !bundleString.isEmpty();
                            String fileString = this.options.filename.trim();
                            boolean fileOnly = !fileString.isEmpty();
                            ArrayList<String> bundleList;




                if (fileOnly){
                    bundleList = getbundleNames(fileString);
                    ArrayList<String> origList = bundleList;
                    ArrayList<String> iterList = bundleList;

                    if (drawGroup.equalsIgnoreCase("set")){

                    for (String list : bundleList ) {
                          if (pkgInfo.exportedBy.contains(list)) {

                          for (int k=0; k<pkgInfo.importedBy.length; k++) {

                              for (String innerList : iterList){

                                if (pkgInfo.importedBy[k].contains(innerList)){

                                    String[] var = {pkgInfo.importedBy[k]};
                                    generateDotEdge(var, pkgInfo.exportedBy,
                                pkgInfo.packageName);

                                }

                              }
                           iterList = origList;
                      }

                    }

                }

                }
                    else if (drawGroup.equalsIgnoreCase("all")){

                       for (String mylist : bundleList ) {

                        if (pkgInfo.exportedBy.contains(mylist)) {
                            generateDotEdge(pkgInfo.importedBy, pkgInfo.exportedBy,
                                    pkgInfo.packageName);
                        }

                        for (int t=0; t<pkgInfo.importedBy.length; t++) {
                              if (pkgInfo.importedBy[t].contains(mylist)) {
                                  String[] temp = {pkgInfo.importedBy[t]};

                            generateDotEdge(temp,pkgInfo.exportedBy,
                                    pkgInfo.packageName);
                        }
                        }

                       }

                    }

                }
                else {


                 if (bundleOnly) {


                     if (drawoption.equalsIgnoreCase("imports")){

                                        if (pkgInfo.exportedBy.contains(bundleString)) {
                                           generateDotEdge(pkgInfo.importedBy, pkgInfo.exportedBy,
                                           pkgInfo.packageName);
                                        }

                     }
                     else if (drawoption.equalsIgnoreCase("exports")){

                                         for (int c=0; c<pkgInfo.importedBy.length; c++) {
                                          if (pkgInfo.importedBy[c].contains(bundleString)) {
                                           String[] temp = {pkgInfo.importedBy[c]};

                                           generateDotEdge(temp,pkgInfo.exportedBy,
                                pkgInfo.packageName);
                                       }
                                    }

                     }

                     else if (drawoption.equalsIgnoreCase("both")) {




                        if (pkgInfo.exportedBy.contains(bundleString)) {
                            generateDotEdge(pkgInfo.importedBy, pkgInfo.exportedBy,
                                    pkgInfo.packageName);
                        }

                        for (int l=0; l<pkgInfo.importedBy.length; l++) {
                              if (pkgInfo.importedBy[l].contains(bundleString)) {
                                  String[] temp = {pkgInfo.importedBy[l]};

                            generateDotEdge(temp,pkgInfo.exportedBy,
                                    pkgInfo.packageName);
                        }
                        }

                     }

                 }

                else{

                if (matchNeeded) {
                    if (pkgInfo.exportedBy.contains(matchString)) {
                        generateDotEdge(pkgInfo.importedBy, pkgInfo.exportedBy,
                                pkgInfo.packageName);
                    }
                } else {
                    generateDotEdge(pkgInfo.importedBy, pkgInfo.exportedBy,
                            pkgInfo.packageName);
                }
            }

                }

            }
            generateDotEnd();
        }

	private void debug(String s) {
		if (DEBUG)
			System.err.println(s);
	}

	private void debug(String text, String s) {
		debug(text, new String[] { s });
	}

	private void debug(String text, String[] arr) {
		StringBuffer sb = new StringBuffer(text);
		for (String s : arr) {
			sb.append(s).append(" , ");
		}
		debug(sb.toString());
	}

	public static void main(String[] args) throws Exception {
		DotGenerator.GeneratorOptions options = new DotGenerator.GeneratorOptions();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("java -jar program-name.jar [options...] arguments...");
			parser.printUsage(System.err);
			return;
		}
		new DotGenerator(options);

	}

	// Generate the beginning of the dot file
	private void generateDotStart() {
		this.wireOut.println("digraph  wiring {");
		this.wireOut.println("node [color=grey, style=filled];");
		this.wireOut.println("node [fontname=\"Verdana\", size=\"30,30\"];");
		String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
				DateFormat.SHORT).format(new Date());
		StringBuffer footer = new StringBuffer();
		footer.append("graph [ fontname = \"Arial\", fontsize = 26,style = \"bold\", ");
		footer.append("label = \"\\nGlassFish v3 OSGi bundle wiring relationship diagram");
		if (!this.options.match.trim().isEmpty()) {
			footer.append("\\n Filter: " + this.options.match.trim()
					+ " bundles");
		}
		footer.append("\\nOracle Corporation");
		footer.append("\\n\\nDate: " + date + "\\n\", "
				+ "ssize = \"30,60\" ];");
		this.wireOut.println(footer.toString());
	}

	// Generate a Dot representation for each edge in the graph
	private void generateDotEdge(String[] importedBy, String exportedBy,
			String pkg) {
		if (importedBy.length == 0)
			return;
		for (String s : importedBy) {
			if (!s.equals(exportedBy)) { // remove self-loops for readability
				this.wireOut.println("\"" + s + "\" -> \"" + exportedBy
						+ "\" [label =\"" + pkg + "\"" + "]");
			}
		}
	}

	// End the dot file generation
	private void generateDotEnd() {
		this.wireOut.println("}");
	}

	// Utility class to split the importers representation (space separated) 
	//in wires.xml
	private String[] split(String s) {
		StringTokenizer st = new StringTokenizer(s);
		List<String> l = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			l.add(st.nextToken());
		}
		return l.toArray(new String[] {});
	}
}
