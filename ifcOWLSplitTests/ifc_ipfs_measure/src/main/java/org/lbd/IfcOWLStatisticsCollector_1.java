package org.lbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.UUID;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.ifcrdf.EventBusService;
import org.ifcrdf.messages.SystemErrorEvent;
import org.lbd.ifc.IfcFileMeasurements;
import org.lbd.ifc.IfcOWLFile;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class IfcOWLStatisticsCollector_1 {
	private final EventBus eventBus = EventBusService.getEventBus();
	private File database_file = new File("c:/jo/measures.ttl");

	private final Model jena_model = ModelFactory.createDefaultModel();
	private final Property jena_property_ifcfile;
	private final Property jena_property_ifcowl_triplescount;

	public IfcOWLStatisticsCollector_1() {
		eventBus.register(this);
		this.jena_property_ifcfile = IfcFileMeasurements.jena_property_ifcfile;
		this.jena_property_ifcowl_triplescount = IfcFileMeasurements.jena_property_ifcowl_triplescount;
		if (!database_file.exists())
		{
			return;
		}
		InputStream in;
		try {
			in = new FileInputStream(database_file);
			jena_model.read(in, null, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	Resource ifcfile=null;
	public void addFiles(String directory) {
		try {
			File folder = new File(directory);
			File[] listOfFiles = folder.listFiles();

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					if (listOfFiles[i].getName().toLowerCase().endsWith(".ifc")) {
						Literal abs_filename = this.jena_model.createLiteral(listOfFiles[i].getAbsolutePath());
						if(this.jena_model.containsLiteral(null, jena_property_ifcfile, abs_filename))
						{
							System.out.println(listOfFiles[i].getAbsolutePath()+" exists already");
							continue;
						}
						String uniqueID = UUID.randomUUID().toString();
						this.ifcfile = this.jena_model.createResource("http://measurements/"+uniqueID);
						IfcOWLFile ifcowl = new IfcOWLFile(listOfFiles[i].getAbsolutePath());
						if (ifcowl.getTriples_count() > 0) {
							
							this.ifcfile.addLiteral(this.jena_property_ifcfile, abs_filename);
							this.ifcfile.addLiteral(this.jena_property_ifcowl_triplescount,ifcowl.getTriples_count());
						}
						else
						{
							this.ifcfile.addLiteral(this.jena_property_ifcfile, abs_filename);
							this.ifcfile.addLiteral(this.jena_property_ifcowl_triplescount,"-1");
						}
						save();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

    @Subscribe
    public void handleEvent(final SystemErrorEvent event) {
        System.out.println("error: " + event.getStatus_message());
        this.ifcfile.addLiteral(IfcFileMeasurements.jena_property_ifc2rdf_error,event.getStatus_message());
        
    }
   
	public void save() {

		OutputStream out;
		try {
			out = new FileOutputStream(database_file);
			jena_model.write(out, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {		
		IfcOWLStatisticsCollector_1 m = new IfcOWLStatisticsCollector_1();
		m.addFiles("c:/ifc/");

	}

}
