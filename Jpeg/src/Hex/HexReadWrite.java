package Hex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HexReadWrite {

    private static final String NEW_LINE = System.lineSeparator();
    private static final String UNKNOWN_CHARACTER = ".";

    public static void main(String[] args) throws IOException {

        String file = "src/resource/1.jpg";
        String s = convertFileToHex(Paths.get(file));
        System.out.println(s);
    }
    
    public static void createFile(String data) {
    	
    	try {
    	 // 1. ���� ��ü ����
        File file = new File("src/resource/test.jpg");

        // 2. ���� ���翩�� üũ �� ����
        if (!file.exists()) {
            file.createNewFile();
        }

        // 3. Buffer�� ����ؼ� File�� write�� �� �ִ� BufferedWriter ����
        FileWriter fw = new FileWriter(file);
        BufferedWriter writer = new BufferedWriter(fw);

        // 4. ���Ͽ� ����
        writer.write(data);

        // 5. BufferedWriter close
        writer.close();

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
    }
    public static String convertFileToHex(Path path) throws IOException {

        if (Files.notExists(path)) {
            throw new IllegalArgumentException("File not found! " + path);
        }

        StringBuilder result = new StringBuilder();
        StringBuilder hex = new StringBuilder();
        StringBuilder input = new StringBuilder();
        StringBuilder data = new StringBuilder();
        
        int count = 0;
        int value;

        // path to inputstream....
        try (InputStream inputStream = Files.newInputStream(path)) {

            while ((value = inputStream.read()) != -1) {
            	
                hex.append(String.format("%02X ", value));
                data.append(String.format("%02X ", value));
                //If the character is unable to convert, just prints a dot "."
                if (!Character.isISOControl(value)) {
                    input.append((char) value);
                } else {
                    input.append(UNKNOWN_CHARACTER);
                }

                // After 15 bytes, reset everything for formatting purpose
                if (count == 14) {
                    result.append(String.format("%-60s | %s%n", hex, input));
                    hex.setLength(0);
                    input.setLength(0);
                    count = 0;
                } else {
                    count++;
                }

            }

            // if the count>0, meaning there is remaining content
            if (count > 0) {
                result.append(String.format("%-60s | %s%n", hex, input));
            }

        }
        createFile(data.toString());
        return result.toString();
    }

}
