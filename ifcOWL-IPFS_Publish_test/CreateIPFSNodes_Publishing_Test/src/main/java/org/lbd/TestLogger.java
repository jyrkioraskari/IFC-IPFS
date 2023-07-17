package org.lbd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public abstract class TestLogger {
	protected List<String> timelog=new LinkedList<>();  
	protected  final DateFormat df = new SimpleDateFormat("yyyyMMdd");
	protected  String reportDate;

	public TestLogger()
	{
		this.reportDate=df.format(new Date());
	}

	synchronized protected void addLog(String txt)
	{
		timelog.add(txt);
	}


	protected void writeToFile(String txt) {

		try {
			PrintWriter pw = new PrintWriter(
					new FileOutputStream(new File("Testlog_"+reportDate + ".txt"), true /* append = true */));
			pw.write(txt);
			pw.write("\n");
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
