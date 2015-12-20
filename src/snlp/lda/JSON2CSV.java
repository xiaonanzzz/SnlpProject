package snlp.lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;


class JSON2CSV{
	
	public static void main(String[] args) throws IOException, IOException {
		String fileAddress="/Users/QimingChen/Downloads/yelp_dataset_challenge_academic_dataset/yelp_academic_dataset_business.json";
		List<String> business_id= new ArrayList<String>();
		
		try(BufferedReader br = new BufferedReader(new FileReader(fileAddress))) {
		    for(String line; (line = br.readLine()) != null; ) {
		    	
		    	
		    	JSONObject obj = new JSONObject(line);
		    	Vector<String> words;
		    	if (obj.getJSONArray("categories")!=null) {
		    		
					//System.out.println(obj.getJSONArray("categories"));
					
					JSONArray input=obj.getJSONArray("categories");
				
			    	words= new Vector<String>();
			    	for (int i = 0; i < input.length(); i++) {
						words.add(input.get(i).toString());
					}
			    	
			    	if (words.contains("Restaurants")) {
						System.out.println("got");
					}
			    	else {
						continue;
					}
			    	
			    	try {
		    			//write after the txt
			    	    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/Users/QimingChen/Desktop/restaurant_id1", true)));
			    	    out.println(obj.getString("business_id"));
			    	    out.close();
			    	} catch (IOException e) {
			    	    //exception handling left as an exercise for the reader
			    	}
		    	
		    	}
		    	
		    }
		}
	}
}