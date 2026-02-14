package fjnu.edu.common.utils.excle;

import java.io.IOException;
import java.util.ArrayList;

//import foundation.file.ExcelUtil;

public class ExcelRead {
	public static void main(String[] args) throws IOException {
		ExcelRead excelRead=new ExcelRead();
		excelRead.execute();
	}
	public void execute() throws IOException{
		System.out.println("****ExcelRead  START******");
		String fileNameString="C:\\Users\\Administrator\\Desktop\\code\\"+"result.xlsx";
		ExcelUtil excelUtil=new ExcelUtil(fileNameString);
		ArrayList<ArrayList<String>> data=excelUtil.readDataRows(String.valueOf(1));
		for(ArrayList<String>  d:data){
			System.out.println(d);
		}
		System.out.println("****ExcelRead END******");
	}

}
