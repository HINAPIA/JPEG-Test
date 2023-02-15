package Hex;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

public class HexDump {

	// byte �迭�� ������ ���� �̸��� �Է� �޾� �ش� ���� �̸����� ���Ϸ� �����ϴ� �Լ�
	public static void WriteFile(byte[] bytes, String saveFilePath) {
		 try {
			 
	            // 1. ���� ��ü ����
	            File file = new File(saveFilePath);
	 
	            // 2. ���� ���翩�� üũ �� ����
	            if (!file.exists()) {
	                file.createNewFile();
	            }
	 
	            // 3. Writer ����
	            FileOutputStream fos = new FileOutputStream(file);
	 
	            // 4. ���Ͽ� ����
	            fos.write(bytes);
	 
	            // 5. FileOutputStream close
	            fos.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	}
	
	// filePath�� �޾� �ش� ��ο� �ִ� ������ �а� �����͸� Hex �ڵ�� �ٲپ� txt���Ϸ� �����ϴ� �Լ�
	public static void bytesToText(String filePath, String SaveFilePath)  throws IOException {
		File sfile = new File(filePath);
        FileInputStream fileis = new FileInputStream(sfile);
        BufferedInputStream bis = new BufferedInputStream(fileis);
        //buffer �Ҵ�
        byte[] arrByte = new byte[bis.available()];
        StringBuilder data = new StringBuilder();
        int ioffs = 0; // ����(����)
        int iLine; // ����
        String space = "   "; // �ڸ��� ���� ����

        System.out.println("iLine:" + ((iLine = bis.read(arrByte))));

        if (iLine > 0) {        

            int end = (int) (iLine / 16) + (iLine % 16 != 0 ? 1 : 0); //��ü �ټ��� ����.
            System.out.println("end:" + end);

            for (int i = 0; i < end; i++) {
            
                //���� ���
                System.out.format("%08X: ", ioffs); // Offset : ���� ���
                
                //��籸��
                for (int j = ioffs; j < ioffs + 16; j++) { //16�� ���

                    if (j < iLine) {
                        //System.out.format("%02X ", arrByte[j]); // ��� �� 2����� , %x 16����
                        data.append(String.format("%02x ", arrByte[j]));
                    } else {                        
                        System.out.print(space);
                    }

                }// for
                
                ioffs += 16; //������ ����.
            }
        }
        
        bis.close();
        fileis.close();
        WriteFile(data.toString().getBytes(), SaveFilePath);
        System.out.print("���� ���� �Ϸ�");
        
	}
	
	public static byte[] getBytes(String filePath) {
	
		byte[] byteFile = null;
		try {
			byteFile = Files.readAllBytes(new File(filePath).toPath());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return byteFile;


	}
	
	public static void readFile(String filePath) throws IOException{
		BufferedReader reader = new BufferedReader(
	            new FileReader(filePath)
	        );
	 
	        String str;
	        while ((str = reader.readLine()) != null) {
	            System.out.println(str);
	        }
	 
	        reader.close();
	}
	public static void stringTobytes (String hexString) {

	     byte[] ans = new byte[hexString.length() / 2];	       
	     for (int i = 0; i < ans.length; i++) {
	    	 int index= 0;
	    	 if(i != 0) {
	    		 index = i * 3;
	    		 if(index >= ans.length) break;
	    	 }     
	           // Using parseInt() method of Integer class
	         int val = Integer.parseInt(hexString.substring(index, index + 2), 16);
	         ans[i] = (byte)val;
	     }
	         
	     // Printing the required Byte Array
	     System.out.print("Byte Array : ");
	     System.out.print("���� ���� �Ϸ�");
	     WriteFile(ans, "src/resource/result/result test3.jpg");
	}
	
	public static void saveFile(String filePath) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
       	     new FileReader(filePath) 	     
       	);
       String data = "",str;
       
       System.out.println("�б� ��");
       while ((str = bufferedReader.readLine()) != null) {
       	data += str;
       	//System.out.print(data);
       }
       System.out.println("�б� �Ϸ�");
       
       stringTobytes(data);
	}
    public static void main(String[] args) throws IOException {
    	
    	
        Path path =  Paths.get("src/resource/result/add.txt");
        
        String filePath = "src/resource/result/result test2.txt";  
        //String filePath = "src/resource/3.jpg";  
       // String SaveFilePath = "src/resource/result/3.txt";
        
        saveFile(filePath);
        //bytesToText(filePath,SaveFilePath);
        // System.out.println(Files.readString(filePath));
        //byte [] bytes = getBytes(filePath);
        //byte [] bytes = new java.math.BigInteger(Files.readString(filePath), 16).toByteArray();
        //WriteFile(bytes, "src/resource/result/3-1.txt");
       // byte [] imgBytes = readBytes(filePath);
        //bytesToText(filePath);
        
    }

}
