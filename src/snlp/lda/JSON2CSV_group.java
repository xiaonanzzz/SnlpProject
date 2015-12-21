package snlp.lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class JSON2CSV_group{
	
	public static void main(String[] args) throws IOException, IOException {
		
		List<String> restaurant_id=new ArrayList<String>();
		try(BufferedReader br = new BufferedReader(new FileReader("/Users/QimingChen/Desktop/restaurant_id"))) {
			 for(String line; (line = br.readLine()) != null; ) {
				 restaurant_id.add(line);
			 }
		}
		
		HashMap<String, Integer> id2Star=new HashMap<String, Integer>();
		try(BufferedReader br = new BufferedReader(new FileReader("/Users/QimingChen/Downloads/yelp_dataset_challenge_academic_dataset/yelp_academic_dataset_business.json"))) {
			 for(String line; (line = br.readLine()) != null; ) {
				 JSONObject obj = new JSONObject(line);
				 id2Star.put(obj.getString("business_id"), obj.getInt("stars"));
			 }
		}
		
		
		String fileAddress="/Users/QimingChen/Downloads/yelp_dataset_challenge_academic_dataset/yelp_academic_dataset_review.json";
		List<String> business_id= new ArrayList<String>();
		int i=1;
		try(BufferedReader br = new BufferedReader(new FileReader(fileAddress))) {
		    for(String line; (line = br.readLine()) != null; ) {
		    	System.out.println(i++);
		    	
		    	JSONObject obj = new JSONObject(line);
		    	
		    	String this_business=obj.getString("business_id");
		    	if (!restaurant_id.contains(this_business)) {
					continue;
				}
		    	
		    	if (business_id.contains(this_business)) {
		    		try {
		    			//write after the txt
			    	    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/Users/QimingChen/Desktop/Yelp-Star/"+this_business, true)));
			    	    out.println(obj.getInt("stars"));
			    	    out.close();
			    	} catch (IOException e) {
			    	    //exception handling left as an exercise for the reader
			    	}
				}else{
					business_id.add(this_business);
					PrintWriter out = new PrintWriter("/Users/QimingChen/Desktop/Yelp-Star/"+this_business);
					out.println(id2Star.get(this_business));
			    	out.println(obj.getInt("stars"));
			    	out.close();
				}
		    }
		}
	}
}