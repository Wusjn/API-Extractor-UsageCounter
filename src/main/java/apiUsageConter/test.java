package apiUsageConter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

public class test {
    public int a;
    public test(int a){
        this.a = a;
    }
    public void incrementByOne(){
        a += 1;
    }



    public static void main(String[] args) {
        test t = new test(1);
        t.a = 1;
        t.incrementByOne();
        Workbook wb = new HSSFWorkbook();
        wb.createCellStyle();
        IndexedColors.RED.getIndex();
    }
}
