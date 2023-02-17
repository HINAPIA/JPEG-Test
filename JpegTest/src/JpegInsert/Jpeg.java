package JpegInsert;

import java.io.*;
import java.nio.file.Files;

public class Jpeg {
    public static final String SOI = "ff d8";
    public static final String SOF0 = "ff c0";
    public static final String SOF1 = "ff c1";
    public static final String SOS = "ff da";
    public static final String APP0 = "ff e0";
    public static final String APP1 = "ff e1";
    public static final String EOI = "ff d9";
    // Destination JPEG 파일에  Source 파일들의 Frame을 집어넣는 함수

    byte[] destByte = null;
    byte[][] extractSourceBytes = null;

    public void insertFramesToJpeg(String destPath, String[] sourcePaths , int sourceLegnth) throws IOException {
        //byte[] destByte = null;
        byte[][] sourceBytes = new byte[sourceLegnth][];

        extractSourceBytes = new byte[sourceLegnth][];
        byte [] resultBytes = null;
        // 1. 파일들의 바이트 배열 얻기
        destByte = getBytes(destPath);
        for(int index =0; index< sourceLegnth; index++){
            sourceBytes[index] = getBytes(sourcePaths[index]);
        }

        // 2. source files의 main frame을 추출
        for(int index = 0; index < sourceBytes.length;index++){
            extractSourceBytes[index] = extractAreaInJPEG(sourceBytes[index], SOF0, EOI);
        }
        //3. 합쳐질 사진에 추출한 프레임을 삽입
        resultBytes = injectFramesToJPEG(destByte,extractSourceBytes);

        //4. 파일로 저장
        writeFile(resultBytes, "src/JpegInsert/resource/result/result.jpg");
        bytesToText("src/JpegInsert/resource/result/result.jpg", "src/JpegInsert/resource/result/result.txt");
    }


    //  (Frame이 여러개인 JPEG 대상) 메인 프레임을 바꾸는 함수
    public void changeMainFrame (int sofNum){

        byte [] resultBytes = null;

        // Section1. 함수를 적용할 수 있는 JPEG 인지 확인
        if (destByte == null || extractSourceBytes == null){
            System.out.println("Error: Required data does not exist");
            return;
        }

        // Section2. 현재 메인프레임이 시작되는 위치(sof0Idx) 알아내기




        // Section3. 메인프레임으로 설정할 SOFn 의 데이터를 해당 위치(sof0Idx)로 옮기고 extract 배열에 원본 데이터 넣기


        // Section4. 변경된 destByte에 변경된

        // Section5. 파일로 저장



    } // end of func changeMainFrame..

    // Dest 파일 바이너리 데이터에 Source 파일들의 메인 프레임(SOF0 ~ EOI) 바이너리 데이터를 넣는 함수
    public byte [] injectFramesToJPEG(byte[] destByte,byte[][] extractSourceBytes ) throws IOException {
        byte [] resultBytes;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // 1. Stream에 Dest파일의 바이너리 데이터를 write
        outputStream.write(destByte);
        for(int i=0; i< extractSourceBytes.length; i++){
            // 2. 추출한 Source 파일의 메
            byte [] frameByte = new byte [(extractSourceBytes[i].length) -2];
            //SOF0 마커 삭제하여
            System.arraycopy(extractSourceBytes[i], 2, frameByte, 0,extractSourceBytes[i].length -2);
            //SOFn 마커 삽입
            String marker = "c"+(i+1);
            outputStream.write((byte)Integer.parseInt("ff", 16));
            outputStream.write((byte)Integer.parseInt(marker, 16));
            //SOFn마커를 제외한 frame 데이터 write - EOI 포함
            outputStream.write(frameByte);

//            //EOI 삽입
//            outputStream.write((byte)Integer.parseInt("ff", 16));
//            outputStream.write((byte)Integer.parseInt("d9", 16));
        }


        resultBytes = outputStream.toByteArray();
        return resultBytes;
    }
    //JPEGFile에서 startMarker가 나오는 부분부터 endMarker가 나오기 전까지 추출하여 byte []로 리턴하는 함수
    public byte[] extractAreaInJPEG(byte [] jpegBytes ,String startMarker, String endMarker) throws IOException {
        String hexString1,hexString2,hexString;
        byte [] resultBytes;
        int startIndex =0;
        int endIndex = jpegBytes.length;
        int startCount =0;
        int endCount =0;

        startMarker = SOF0;
        endMarker = EOI;

        int startMax =1;
        int endMax =2;
        //썸네일의 SOF0가 먼저 나와서 2번 해당 마커를 찾도록
        if(startMarker.equals(SOF0)) startMax = 2;

        for (int i = 0; i < jpegBytes.length-1; i++) {

            hexString1 = String.format("%02x", jpegBytes[i]);
            hexString2 = String.format("%02x", jpegBytes[i + 1]);
            hexString = hexString1 + " " + hexString2;
            //System.out.println("start hex string : " + hexString);
            if (hexString.equals(startMarker)) {
                startCount++;
                if(startCount == startMax){
                    startIndex = i;
                    System.out.println("start hex string : " + hexString + ", startInex : " + startIndex);
                }
            }
            if (hexString.equals(endMarker)) {
                endCount++;
                if(endCount == endMax){
                    endIndex = i;
                    System.out.println("end hex string : " + hexString + ", endIndex : " + endIndex);
                }
            }
        }

        // 추출
        resultBytes = new byte[endIndex-startIndex+2];
        // start 마커부터 end 마커를 포함한 영역까지 복사해서 resultBytes에 저장
        System.arraycopy(jpegBytes, startIndex, resultBytes, 0,endIndex-startIndex+2);
        //writeFile(resultBytes,"src/JpegInsert/resource/result/test.jpg");
        //bytesToText("src/JpegInsert/resource/result/test.jpg", "src/JpegInsert/resource/result/test2.txt");
        return resultBytes;

    }
    // byte 배열과 저장할 파일 이름을 입력 받아 해당 파일 이름으로 파일로 저장하는 함수
    public  void writeFile(byte[] bytes, String saveFilePath) {
        try {

            // 1. 파일 객체 생성
            File file = new File(saveFilePath);
            // 2. 파일 존재여부 체크 및 생성
            if (!file.exists()) {
                file.createNewFile();
            }
            // 3. Writer 생성
            FileOutputStream fos = new FileOutputStream(file);

            // 4. 파일에 쓰기
            fos.write(bytes);

            // 5. FileOutputStream close
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // filePath를 받아 해당 경로에 있는 파일을 읽고 데이터를 Hex 코드로 바꾸어 txt파일로 저장하는 함수
    public void bytesToText(String filePath, String SaveFilePath)  throws IOException {
        File sfile = new File(filePath);
        FileInputStream fileis = new FileInputStream(sfile);
        BufferedInputStream bis = new BufferedInputStream(fileis);
        //buffer 할당
        byte[] arrByte = new byte[bis.available()];
        StringBuilder data = new StringBuilder();
        int ioffs = 0;// 번지(라인)
        int iLine;// 길이
        String space = "   "; // 자리수 맞출 공백

        System.out.println("iLine:" + ((iLine = bis.read(arrByte))));

        if (iLine > 0) {

            int end = (int) (iLine / 16) + (iLine % 16 != 0 ? 1 : 0); //��ü �ټ��� ����.
            System.out.println("end:" + end);

            for (int i = 0; i < end; i++) {

                //번지 출력
              //  System.out.format("%08X: ", ioffs); // Offset : ���� ���
                //헥사구역
                for (int j = ioffs; j < ioffs + 16; j++) { //16�� ���
                    if (j < iLine) {
                        //System.out.format("%02X ", arrByte[j]); // ��� �� 2����� , %x 16����
                        data.append(String.format("%02x ", arrByte[j]));
                    } else {
                        System.out.print(space);
                    }
                }// for
                ioffs += 16; //번지수 증가.
            }
        }

        bis.close();
        fileis.close();
        writeFile(data.toString().getBytes(), SaveFilePath);
        System.out.print("파일 저장 완료");

    }

    //파일경로를 받아 해당 파일을 바이트 배열로
    public byte[] getBytes(String filePath) {

        System.out.println("getBytes [filePath] : " + filePath);
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
    public void stringTobytes (String hexString) {

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
        System.out.print("파일 저장 완료");
        writeFile(ans, "src/resource/result/result test3.jpg");
    }

    public void saveFile(String filePath) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new FileReader(filePath)
        );
        String data = "",str;

        System.out.println("읽기 전");
        while ((str = bufferedReader.readLine()) != null) {
            data += str;
            //System.out.print(data);
        }
        System.out.println("읽기 완료");

        stringTobytes(data);
    }
}
