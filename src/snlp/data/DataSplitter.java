package snlp.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Split data to training, validate and test data set
 * @author Alex
 *
 */
public class DataSplitter {

	String filePath;
	String outputPath;
	
	public DataSplitter(String filePath, String outputPath){
		this.filePath = filePath;
		this.outputPath = outputPath;
	}
	
	/**
	 * Randomly split the training data line by line (ONE LINE as one entry)
	 * @param outputName The output file are named by this name, 
	 * eg. given "-data.json", the output file is "train-data.json", "validate-data.json", "test-data.json"
	 * @param validateRatio [0, 1)
	 * @param testRatio [0, 1)
	 */
	public void splitByLines(String outputName, double validateRatio, double testRatio){
		List<String> data = new ArrayList<String>();
		try
		{
			BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
			for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()){
				data.add(line);
			}
			bufferedReader.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		System.out.printf("Read %d lines of data.\n", data.size());
		
		int testCount = (int)(data.size() * testRatio);
		assert(testCount >= 0);
		
		int validateCount = (int)(data.size() * validateRatio);
		assert(validateCount >= 0);
		assert(1 > testRatio + validateRatio);
		
		System.out.printf("%d lines as test data.\n", testCount);
		if(testCount > 0)
		try {
			FileWriter fileWriter = new FileWriter(outputPath + "/test" + outputName);
			for (int i = 0; i < testCount; i++){
				int index = (int) (Math.random() * data.size());

				fileWriter.write(data.remove(index));
				fileWriter.write("\n");
			}
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		System.out.printf("%d lines as validate data.\n", validateCount);
		if (validateCount > 0)
		try {
			
			FileWriter fileWriter = new FileWriter(outputPath + "/validate" + outputName);
			for (int i = 0; i < validateCount; i++){
				int index = (int) (Math.random() * data.size());

				fileWriter.write(data.remove(index));
				fileWriter.write("\n");
			}
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.printf("%d lines as training data.\n", data.size());
		try {
			
			FileWriter fileWriter = new FileWriter(outputPath + "/train" + outputName);
			for (String line : data){

				fileWriter.write(line);
				fileWriter.write("\n");
			}
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		DataSplitter dataSplitter = new DataSplitter("./dirty data/rest-review-stars.json", "./dirty data");
		dataSplitter.splitByLines("rest-review-stars.json", 0.1, 0.1);
	}

}
