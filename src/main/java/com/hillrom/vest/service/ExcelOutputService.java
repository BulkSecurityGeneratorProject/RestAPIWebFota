package com.hillrom.vest.service;

import static com.hillrom.vest.config.Constants.DATE;
import static com.hillrom.vest.config.Constants.DEVICE_ADDRESS;
import static com.hillrom.vest.config.Constants.DURATION;
import static com.hillrom.vest.config.Constants.EVENT;
import static com.hillrom.vest.config.Constants.FREQUENCY;
import static com.hillrom.vest.config.Constants.HMR;
import static com.hillrom.vest.config.Constants.HUB_ADDRESS;
import static com.hillrom.vest.config.Constants.PATIENT_ID;
import static com.hillrom.vest.config.Constants.PRESSURE;
import static com.hillrom.vest.config.Constants.SERIAL_NO;
import static com.hillrom.vest.config.Constants.TIME;
import static com.hillrom.vest.config.Constants.HILLROM_ID;
import static com.hillrom.vest.config.Constants.WIFIorLTE_SERIAL_NO;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.hillrom.vest.domain.PatientVestDeviceData;
import com.hillrom.vest.domain.PatientVestDeviceDataMonarch;


@Service
public class ExcelOutputService {

	private static final Logger log = LoggerFactory.getLogger(ExcelOutputService.class);

	public void createExcelOutputExcel(HttpServletResponse response,List<PatientVestDeviceData> deviceEventsList) throws IOException{
		log.debug("Received Device Data "+deviceEventsList);
		
		response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=TherapyReport.xls");
        
        HSSFWorkbook workBook = new HSSFWorkbook();
        HSSFSheet excelSheet = workBook.createSheet("Therapy Report");
        /* Freeze top row alone */
        excelSheet.createFreezePane(0,1);
    	String[] header = { PATIENT_ID,DATE,TIME, EVENT,
				SERIAL_NO, DEVICE_ADDRESS, HUB_ADDRESS, FREQUENCY, PRESSURE,DURATION,HMR};
        setExcelHeader(excelSheet,header);
        setExcelRows(workBook, excelSheet, deviceEventsList);
        autoSizeColumns(excelSheet,11);
        
        workBook.write(response.getOutputStream());
        response.getOutputStream().flush();
	}
	
	public void createExcelOutputExcelForMonarch(HttpServletResponse response,List<PatientVestDeviceDataMonarch> deviceEventsList) throws IOException{
		log.debug("Received Device Data "+deviceEventsList);
		
		response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=TherapyReport.xls");
        
        HSSFWorkbook workBook = new HSSFWorkbook();
        HSSFSheet excelSheet = workBook.createSheet("Therapy Report");
        /* Freeze top row alone */
        excelSheet.createFreezePane(0,1);
    	String[] header = { HILLROM_ID,DATE,TIME, EVENT,
				SERIAL_NO, WIFIorLTE_SERIAL_NO, FREQUENCY, PRESSURE,DURATION,HMR};
        setExcelHeader(excelSheet,header);
        setExcelRowsForMonarch(workBook, excelSheet, deviceEventsList);
        autoSizeColumns(excelSheet,11);
        
        workBook.write(response.getOutputStream());
        response.getOutputStream().flush();
	}
	
	public void setExcelHeader(HSSFSheet excelSheet,String ...headerNames) {
		HSSFRow excelHeader = excelSheet.createRow(0);
		int cellCount = 0;
		for(String headerName : headerNames){
			excelHeader.createCell(cellCount++).setCellValue(headerName);
		}
	}
	
	public void setExcelRows(HSSFWorkbook workBook,HSSFSheet excelSheet, List<PatientVestDeviceData> deviceEventsList){
		int record = 1;
		HSSFCellStyle dateStyle = createCellStyle(workBook,"m/d/yy");
		HSSFCellStyle timeStyle = createCellStyle(workBook,"h:mm AM/PM");
		for (PatientVestDeviceData deviceEvent : deviceEventsList) {
			HSSFRow excelRow = excelSheet.createRow(record++);
			excelRow.createCell(0).setCellValue(deviceEvent.getPatientBlueToothAddress());
			
			HSSFCell dateCell = excelRow.createCell(1);
			dateCell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
			dateCell.setCellValue(deviceEvent.getDate().toDate());
			dateCell.setCellStyle(dateStyle);
			
			HSSFCell timeCell = excelRow.createCell(2);
			timeCell.setCellValue(deviceEvent.getDate().toDate());
			timeCell.setCellStyle(timeStyle);
			
			excelRow.createCell(3).setCellValue(deviceEvent.getEventId());
			excelRow.createCell(4).setCellValue(deviceEvent.getSerialNumber());
			excelRow.createCell(5).setCellValue(deviceEvent.getBluetoothId());
			excelRow.createCell(6).setCellValue(deviceEvent.getHubId());
			excelRow.createCell(7).setCellValue(deviceEvent.getFrequency());
			excelRow.createCell(8).setCellValue(deviceEvent.getPressure());
			excelRow.createCell(9).setCellValue(deviceEvent.getDuration());
			excelRow.createCell(10).setCellValue(deviceEvent.getHmrInHours());
		}
	}
	
	public void setExcelRowsForMonarch(HSSFWorkbook workBook,HSSFSheet excelSheet, List<PatientVestDeviceDataMonarch> deviceEventsList){
		int record = 1;
		HSSFCellStyle dateStyle = createCellStyle(workBook,"m/d/yy");
		HSSFCellStyle timeStyle = createCellStyle(workBook,"h:mm AM/PM");
		for (PatientVestDeviceDataMonarch deviceEvent : deviceEventsList) {
			HSSFRow excelRow = excelSheet.createRow(record++);
			excelRow.createCell(0).setCellValue(deviceEvent.getPatient().getHillromId());
			
			HSSFCell dateCell = excelRow.createCell(1);
			dateCell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
			dateCell.setCellValue(deviceEvent.getDate().toDate());
			dateCell.setCellStyle(dateStyle);
			
			HSSFCell timeCell = excelRow.createCell(2);
			timeCell.setCellValue(deviceEvent.getDate().toDate());
			timeCell.setCellStyle(timeStyle);
			
			excelRow.createCell(3).setCellValue(deviceEvent.getEventCode());
			excelRow.createCell(4).setCellValue(deviceEvent.getSerialNumber());
			if(Objects.nonNull(deviceEvent.getDevWifi()) && Objects.nonNull(deviceEvent.getDevLte())){
			excelRow.createCell(5).setCellValue(deviceEvent.getDevWifi());
			}
			else if(Objects.isNull(deviceEvent.getDevWifi()) && Objects.nonNull(deviceEvent.getDevLte())){
			excelRow.createCell(5).setCellValue(deviceEvent.getDevLte());
			}
			else{
			excelRow.createCell(5).setCellValue(deviceEvent.getDevWifi());
			}
			//excelRow.createCell(6).setCellValue("");
			excelRow.createCell(6).setCellValue(deviceEvent.getFrequency());
			excelRow.createCell(7).setCellValue(deviceEvent.getIntensity());
			excelRow.createCell(8).setCellValue(deviceEvent.getDuration());
			excelRow.createCell(9).setCellValue(deviceEvent.getHmrInHours());
		}
	}
	
	
	
	public HSSFCellStyle createCellStyle(HSSFWorkbook workBook,String dataFormat){
		HSSFCellStyle hssfCellStyle = workBook.createCellStyle();
		if(Objects.nonNull(dataFormat)){
			CreationHelper createHelper = workBook.getCreationHelper();
	        // Set the date format of date
			hssfCellStyle.setDataFormat(createHelper.createDataFormat().getFormat(
	                dataFormat));
			hssfCellStyle.setWrapText(true);
		}
		return hssfCellStyle;
	}
	
	public void autoSizeColumns(HSSFSheet excelSheet,int columnCount){
		for (int i = 0; i < columnCount; i++){
			excelSheet.autoSizeColumn(i);
		}
	}
}
