package snlp.data.yelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReviewTopicFile {

	String filePath;
	int dimension = 0;
	
	
	
	/**
	 * @param filePath
	 * @param dimension
	 */
	public ReviewTopicFile(String filePath, int dimension) {
		super();
		this.filePath = filePath;
		this.dimension = dimension;
	}

	public Map<String, double[]> read() throws IOException{
		Map<String, double[]> reviewTopicVectors = new HashMap<String, double[]>();
		
		BufferedReader topicVectorReader = new BufferedReader(new FileReader(this.filePath));
		int count = 0;
		for (String line = topicVectorReader.readLine();
				line != null; line = topicVectorReader.readLine()){
			
			String[] strVec = line.split(" ");
			if (dimension == 0)
				dimension = (strVec.length - 1); // if dimension is not pre-assigned
			if (dimension != (strVec.length - 1) )
				throw new RuntimeException("dimension != strVec.length-1");
			
			double[] topicVec = new double[dimension];
			for (int i = 0; i < dimension; i++){
				topicVec[i] = Double.parseDouble(strVec[i+1]);
			}
			reviewTopicVectors.put(strVec[0], topicVec);
			count++;
			if (count %10000 == 0){
				System.out.println("Read " + count + " lines...");
			}
		}
		System.out.println("Read " + count + " lines!");
		topicVectorReader.close();
		
		return reviewTopicVectors;
	
	}
	
	public void write(Map<String, double[]> reviewVecs) throws IOException{

		FileWriter writer = new FileWriter(this.filePath);
		int count = 0;
		for (Map.Entry<String, double[]> entry : reviewVecs.entrySet()){
			
			writer.write(entry.getKey());
			for (double x : entry.getValue()){
				writer.write(String.format(" %.2E", x));
			}
			writer.write("\n");
			count++;
			if (count %10000 == 0){
				System.out.println("Write " + count + " lines...");
			}
		}
		System.out.println("Write " + count + " lines!");
		writer.close();
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ReviewTopicFile file = new ReviewTopicFile("./topicDistribution/review/reviews1-390.theta", 0);
		ReviewTopicFile newFile = new ReviewTopicFile("./localdata/reviews.topicdist", 0);
		try {
			newFile.write(file.read());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
