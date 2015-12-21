package snlp.lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.MissingFormatArgumentException;
import java.util.Vector;

public class narrowDigitPrecision {
	public static void main(String[] args) throws IOException {
		String fileAddress="/Users/QimingChen/Desktop/data1";
		String inAddress="/Users/QimingChen/Desktop/data1.new";
		int precision=7;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(fileAddress));
			for(String str; (str = br.readLine()) != null;){
				String[] num=str.trim().split("\\s+");
				Vector<String> smaller=new Vector<String>();
				double sum=0;
				for (int i = 1; i < num.length; i++) {
					smaller.add(num[i]);
					double g=Double.parseDouble(num[i]);
					sum=sum+Double.parseDouble(num[i]);
				}
				
				System.out.println(sum);
				
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(inAddress, true)));
		    	out.print(num[0]+ " ");
		    	for (int i = 0; i < smaller.size(); i++) {
		    		String digittemp=smaller.get(i);
		    		if(digittemp.contains("E")==true){
		    			//科学计数法
		    			digittemp=digittemp.substring(0, precision) + digittemp.substring(digittemp.indexOf("E"),digittemp.length());
		    		}else{
		    			digittemp=digittemp.substring(0, precision);
		    		}
					out.print(digittemp+" ");
				}
		    	out.close();
				
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	
}
