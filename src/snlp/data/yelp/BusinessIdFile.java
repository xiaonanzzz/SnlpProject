/**
 * 
 */
package snlp.data.yelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * @author Alex
 *
 */
public class BusinessIdFile {

	String filePath;
	
	
	
	/**
	 * @param filePath
	 */
	public BusinessIdFile(String filePath) {
		super();
		this.filePath = filePath;
	}


	/**
	 * Read business id from given file. One ID, one line.
	 * @return
	 * @throws IOException
	 */
	public String[] readBusinessIds() throws IOException{
		LinkedList<String> busIds = new LinkedList<String>();
		BufferedReader businessIdReader = new BufferedReader(new FileReader(this.filePath));
		for (String businessId = businessIdReader.readLine();
				businessId != null; businessId = businessIdReader.readLine()){
			busIds.add(businessId);
		}
		businessIdReader.close();
		return busIds.toArray(new String[0]);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
