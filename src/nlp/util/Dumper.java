package nlp.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Dumper{

	//Configuration parameters
	String dumpPath = "./dump";
	String dumpName = "dump";
	String dumpDate;
	
	//running data
	PrintStream dumper;
	File dumpFile;
	File dumpFolder;
	
	public Dumper(Dumper prototype, String dumpName){
		this.dumpDate = prototype.dumpDate;
		this.dumpPath = prototype.dumpPath;
		this.dumpName = dumpName;
		
		init();
	}
	
	public Dumper(String dumpName){
		this.dumpName = dumpName;
		init();
	}
	
	public Dumper(String dumpPath, String dumpName){
		this.dumpPath = dumpPath;
		this.dumpName = dumpName;
		init();
	}
	
	public void dumpln(String str){
		dumper.println(str);
	}
	public void dump(String str){
		dumper.print(str);
	}
	
	public void dumpf(String format, Object ... args){
		dumper.printf(format, args);
	}
	
	public String getFolderPath(){
		return dumpFolder.getPath();
	}
	
	void init(){
		if (dumpDate == null){
			Date now = new Date();
			SimpleDateFormat dateFormatter = new SimpleDateFormat("/yyyy-MM-dd/HH-mm-ss");
			dumpDate = dateFormatter.format(now);
		}
		dumpFolder = new File(dumpPath + dumpDate);
		dumpFolder.mkdirs();
		
		dumpFile = new File(dumpFolder.getPath() + "/" + dumpName + ".txt");
		try {
			dumper = new PrintStream(dumpFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
}
