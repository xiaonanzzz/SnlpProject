package project1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;


class JSON2CSV{
	
	public static void main(String[] args) throws IOException, IOException {
		String fileAddress="/Users/QimingChen/Downloads/yelp_dataset_challenge_academic_dataset/yelp_academic_dataset_review.json";
		List<String> business_id= new ArrayList<String>();
		
		try(BufferedReader br = new BufferedReader(new FileReader(fileAddress))) {
		    for(String line; (line = br.readLine()) != null; ) {
		    	
		    	
		    	JSONObject obj = new JSONObject(line);
		    	
		    	String this_business=obj.getString("business_id");
		    	
		    	if (business_id.contains(this_business)) {
		    		try {
		    			//write after the txt
			    	    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/Users/QimingChen/Desktop/Yelp_Review/review"+this_business+".txt", true)));
			    	    out.println(obj.getString("text"));
			    	    out.close();
			    	} catch (IOException e) {
			    	    //exception handling left as an exercise for the reader
			    	}
				}else{
					business_id.add(this_business);
					PrintWriter out = new PrintWriter("/Users/QimingChen/Desktop/Yelp_Review/review"+this_business+".txt");
			    	out.println(obj.getString("text"));
			    	out.close();
				}
		    }
		}
	}
}