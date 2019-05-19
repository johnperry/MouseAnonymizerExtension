package edu.uams.anonymizer;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.plugin.AbstractPlugin;
import org.rsna.ctp.stdstages.anonymizer.dicom.AnonymizerExtension;
import org.rsna.ctp.stdstages.anonymizer.dicom.FnCall;
import org.w3c.dom.Element;

/**
 * An AnonymizerExtension to build structures in DicomObjects.
 */
public class MouseAnonymizerExtension extends AbstractPlugin implements AnonymizerExtension {
	
	static final Logger logger = Logger.getLogger(MouseAnonymizerExtension.class);
	
	/**
	 * IMPORTANT: When the constructor is called, neither the
	 * pipelines nor the HttpServer have necessarily been
	 * instantiated. Any actions that depend on those objects
	 * must be deferred until the start method is called.
	 * @param element the XML element from the configuration file
	 * specifying the configuration of the plugin.
	 */
	public MouseAnonymizerExtension(Element element) {
		super(element);
		logger.info(getID()+" Plugin instantiated");
	}

	/**
	 * Implement the AnonymizerExtension interface
	 * @param fnCall the specification of the function call.
	 * @return the result of the function call.
	 * @throws Exception
	 */
	public synchronized String call(FnCall fnCall) throws Exception {
		/*
		Patient Species Code Sequence (0010,2202):
		> Code Value = "447612001"
		> Coding Scheme Designator = "SCT"
		> Code Meaning = "Mus musculus"

		fnCall arguments:
		[0]: id attribute of the AnonymizerExtension plugin
		[1]: SQ element id (or this)
		[2]: "element=value"
		...

		For example:
		@call(ID,PatientSpeciesCodeSeq,"CodeValue=447612001","CodingSchemeDesignator=SCT","CodeMeaning=Mus muslulus")
		*/
		
		logger.debug("Received call: "+fnCall.getCall());
		
		//Get the fnCall arguments
		String[] args = fnCall.args;
		
		//Get the SQ element ID
		String sqID = args[1].trim();
		int sqTag = sqID.equals("this") ? fnCall.thisTag : DicomObject.getElementTag(sqID);
		if (sqTag == 0) throw new Exception("Unparsable element specification; "+sqID);
		
		//Get the output dataset
		Dataset outDS = fnCall.context.outDS;
		
		//Get the SQ element (in outDS), creating it if necessary
		DcmElement sq = outDS.get(sqTag);
		if (sq == null) sq = outDS.putSQ(sqTag);
		
		//Get the first item dataset, creating it if necessary
		Dataset itemDS = sq.getItem();
		if (itemDS == null) itemDS = sq.addNewItem();
		
		//Create the required elements in itemDS
		for (int i=2; i<args.length; i++) {
			logger.debug("processing "+args[i]);
			if (args[i].startsWith("\"") && args[i].endsWith("\"")) {
				args[i] = args[i].substring(1, args[i].length()-1);
			}
			String[] s = args[i].split("=");
			if (s.length < 2) throw new Exception("Improper assignment: "+args[i]);
			logger.debug("id = \""+s[0]+"\"; value = \""+s[1]+"\"");
			int sTag = DicomObject.getElementTag(s[0].trim());
			if (sTag == 0) throw new Exception("Unparsable element specification; \""+s[0]+"\"");
			itemDS.putXX(sTag, s[1].trim());
		}
			
		//Return an instruction to force the anonymizer to keep the element
		return "@keep()";
	}
}