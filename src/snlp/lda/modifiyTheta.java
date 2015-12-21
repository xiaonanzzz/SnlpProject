package snlp.lda;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class modifiyTheta {
	public static void main(String[] args) throws IOException {
		Vector<String> fileName = new Vector<String>();
		fileName=GibbsSamplingLDA.getFileName("/Users/QimingChen/Desktop/Yelp_Review");
		
		BufferedWriter writer;
		try {
			int count=0;
			writer = new BufferedWriter(new FileWriter("/Users/QimingChen/Desktop/" + "idList"));
			for (int i = 0; i <fileName.size() ; i++) {
				//writer.write(fileName.get(i));
				writer.write(fileName.get(i).substring(44, fileName.get(i).length()-4));
				writer.write("\n");
				count++;
			}
			writer.close();
			System.out.println(count);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
