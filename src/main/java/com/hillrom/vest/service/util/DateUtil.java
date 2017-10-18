package com.hillrom.vest.service.util;

import static com.hillrom.vest.config.Constants.DAY_STRING;
import static com.hillrom.vest.config.Constants.MMddyyyy;
import static com.hillrom.vest.config.Constants.MONTH_STRING;
import static com.hillrom.vest.config.Constants.WEEK_SEPERATOR;
import static com.hillrom.vest.config.Constants.YEAR_STRING;
import static com.hillrom.vest.config.Constants.YYYY_MM_DD;

import java.security.InvalidParameterException;
import java.text.DateFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.hillrom.vest.exceptionhandler.HillromException;
import com.hillrom.vest.util.ExceptionConstants;

public class DateUtil {

	private DateUtil(){
		
	}
	
	/**
	 * Get All LocalDates between two LocalDate
	 * @param from
	 * @param to
	 * @return
	 */
	public static List<LocalDate> getAllLocalDatesBetweenDates(LocalDate from, LocalDate to) {
		LocalDate startDate = from;
		List<LocalDate> dates = new LinkedList<>();
		while(!startDate.isAfter(to)){
			dates.add(startDate);
			startDate = startDate.plusDays(1);
		}
		return dates;
	}
	
	/**
	 * Group List of LocalDate by week of week year
	 * @param dates
	 * @return
	 */
	public static Map<Integer, List<LocalDate>> groupListOfLocalDatesByWeekOfWeekyear(
			List<LocalDate> dates) {
		Map<Integer,List<LocalDate>> groupByWeek = dates.stream().collect(Collectors.groupingBy(LocalDate :: getWeekOfWeekyear));
		return groupByWeek;
	}
	
	/**
	 * Group List of LocalDate by Month Of Year
	 * @param dates
	 * @return
	 */
	public static Map<Integer, List<LocalDate>> groupListOfLocalDatesByMonthOfYear(
			List<LocalDate> dates) {
		Map<Integer,List<LocalDate>> groupByWeek = dates.stream().collect(Collectors.groupingBy(LocalDate :: getMonthOfYear));
		return groupByWeek;
	}
	
	public static LocalDate parseStringToLocalDate(String dateString, String dateFormat) throws HillromException{
		dateFormat = Objects.nonNull(dateFormat) ? dateFormat : YYYY_MM_DD; 
		final DateTimeFormatter dtf = DateTimeFormat.forPattern(dateFormat);
		try{
			return dtf.parseLocalDate(dateString);
		}catch(Exception ex){
			throw new HillromException(ExceptionConstants.HR_600.concat(dateFormat), ex);
		}
    	
	}
	
	
	/** Get Date by Plus or Minus Days
	 * @param days
	 * @return
	 */
	public static LocalDate getPlusOrMinusTodayLocalDate(int days){
		LocalDate today = LocalDate.now();
		if(days > 0){
			return today.plusDays(days);
		}else{
			return today.minusDays(Math.abs(days));
		}
	}
	
	/** Get Date by Plus or Minus Days
	 * @param days
	 * @return
	 */
	public static LocalDate getPlusOrMinusDate(int days,LocalDate date){		
		if(days > 0){
			return date.plusDays(days);
		}else{
			return date.minusDays(Math.abs(days));
		}
	}
	
	/**
	 * Returns today in LocalDate
	 * @return 
	 */
	public static LocalDate getTodayLocalDate(){
		return LocalDate.now();
	}
	
	/**
	 * Return days difference between two Local Dates
	 * @param from
	 * @param to
	 * @return
	 */
	public static int getDaysCountBetweenLocalDates(LocalDate from,LocalDate to){
		return Days.daysBetween(from, to).getDays();
	}
	
	/**
	 * Return days difference between two Local Dates
	 * @param from
	 * @param to
	 * @return
	 */
	public static LocalDate getDateBeforeSpecificDays(LocalDate date, int beforeDays){
		return date.minusDays(Math.abs(beforeDays));
	}
	
	/**
	 * Return LocalDates group by Day Of Week
	 * @param dates
	 * @return
	 */
	public static Map<Integer,List<LocalDate>> groupListOfLocalDatesByDayOfWeek(List<LocalDate> dates){
		return  dates.stream().collect(Collectors.groupingBy(LocalDate :: getDayOfWeek));
	}
	
	/**
	 * convertLocalDateToStringFromat
	 * @param date
	 * @param format
	 * @return
	 */
	public static String convertLocalDateToStringFromat(LocalDate date,String format){
		DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
		return date.toString(formatter);
	}
	
	
	public static String getShortMonthNameByIndex(int index){
		if(index > 0){
			DateFormatSymbols symbols = new DateFormatSymbols();
			return symbols.getShortMonths()[index-1];
		}else{
			throw new ArrayIndexOutOfBoundsException("Month index can't be <= 0");
		}
	}
	
	/**
	 * prepare list of date strings (ex: 23-Feb-16) to be shown x-axis labels
	 * @param dates
	 * @return
	 */
	public static List<String> getDatesStringGroupByDay(List<LocalDate> dates){
		List<String> dateStrings = new LinkedList<>();
		for(LocalDate date: dates){
			dateStrings.add(convertLocalDateToStringFromat(date, MMddyyyy));
		}
		return dateStrings;
	}
	
	/**
	 * prepare list of date strings (ex: week1(from-to)) to be shown x-axis labels
	 * @param dates
	 * @return
	 */
	public static List<String> getDatesStringGroupByWeek(List<LocalDate> dates) {
		List<String> dateStrings = new LinkedList<>();
		int DAYS = 7;
		for (int i = 0; i < dates.size(); i += 7) {
			int lastIndex = i + DAYS > dates.size() ? dates.size() : i + DAYS;
			List<LocalDate> subList = dates.subList(i, lastIndex);
			String fromDate = DateUtil.convertLocalDateToStringFromat(
					subList.get(0), MMddyyyy);
			String toDate = DateUtil.convertLocalDateToStringFromat(
					subList.get(subList.size() - 1), MMddyyyy);
			dateStrings.add(fromDate + WEEK_SEPERATOR
					+ toDate);
		}
		return dateStrings;
	}
	
	/**
	 * prepare list of date strings (ex: Feb'16) to be shown x-axis labels
	 * @param dates
	 * @return
	 */
	public static  List<String> getDatesStringGroupByMonth(List<LocalDate> dates) {
		List<String> dateStrings = new LinkedList<>();
		SortedMap<Integer, List<LocalDate>> groupByYear = new TreeMap<>(dates
				.stream().collect(
						Collectors.groupingBy(LocalDate::getYearOfCentury)));
		for (Integer year : groupByYear.keySet()) {
			Map<Integer, List<LocalDate>> groupByMonth = groupByYear.get(year)
					.stream()
					.collect(Collectors.groupingBy(LocalDate::getMonthOfYear));
			for (Integer month : groupByMonth.keySet()) {
				dateStrings.add(new StringBuilder(DateUtil
						.getShortMonthNameByIndex(month)).append("'")
						.append(year).toString());
			}
		}
		return dateStrings;
	}

	/**
	 * return formatted date string as per pattern, default pattern is dd-MMM-yy
	 * @param dates
	 * @return
	 */
	public static String formatDate(LocalDate date, String pattern) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern(Objects.nonNull(pattern)?pattern:MMddyyyy);
		return date.toString(formatter);
	}
	
	/**
	 * return formatted date string as per pattern, default pattern is dd-MMM-yy
	 * @param dates
	 * @return
	 */
	public static String formatDate(DateTime dateTime, String pattern) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern(Objects.nonNull(pattern)?pattern:MMddyyyy);
		return dateTime.toString(formatter);
	}

	/**
	 * return formatted date string as per pattern, default pattern is dd-MMM-yy
	 * @param dates
	 * @return
	 */
	public static int getPeriodBetweenLocalDates(LocalDate from, LocalDate to,String period) {
		if(Objects.isNull(from) || Objects.isNull(to))
			return 0;
		else{
			Period timePeriod =  new Period(from,to,PeriodType.yearMonthDay());
			if(Objects.isNull(period) || YEAR_STRING.equalsIgnoreCase(period))
				return timePeriod.getYears();
			else if(MONTH_STRING.equalsIgnoreCase(period))
				return timePeriod.getMonths();
			else if(DAY_STRING.equalsIgnoreCase(period))
				return timePeriod.getDays();
			return 0;	
		}
	}
	public static String formatDateWithDaySuffix(DateTime dateTime, String pattern) {
		String datetime = formatDate(dateTime, pattern);
		String suffix = getDayOfMonthSuffix(dateTime.getDayOfMonth());
	    return datetime.replaceFirst(",", suffix+",");
	}	
	
	private static String getDayOfMonthSuffix(final int n) {
	    if(n < 1 && n > 31)
	    	throw new InvalidParameterException();
	    if (n >= 11 && n <= 13) {
	        return "th";
	    }
	    switch (n % 10) {
	        case 1:  return "st";
	        case 2:  return "nd";
	        case 3:  return "rd";
	        default: return "th";
	    }
	}
	//hill-1847
		/**
		 * Returns today in DateTime
		 * @return 
		 */
		public static DateTime getCurrentDateAndTime(){
		Calendar cal = Calendar.getInstance();
		DateTime dateTime = new DateTime(cal.getTime());
		return dateTime;
		}
		//hill-1847
		
		
	   public static Map<String, String> getTimeZoneList() {

	        Map<String, String> sortedMap = new LinkedHashMap<>();

	        List<String> zoneList = new ArrayList<>(ZoneId.getAvailableZoneIds());

	        Map<String, String> result = new HashMap<>();

	        LocalDateTime dt = LocalDateTime.now();

	        for (String zoneId : zoneList) {

	            ZoneId zone = ZoneId.of(zoneId);
	            ZonedDateTime zdt = dt.atZone(zone);
	            ZoneOffset zos = zdt.getOffset();

	            //replace Z to +00:00
	            String offset = zos.getId().replaceAll("Z", "+00:00");

	            result.put(zone.toString(), offset);

	        }

			return(result);
	   }
	   





}
