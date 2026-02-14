package fjnu.edu.common.utils;

import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Description:时间戳转换工具
*/
public class TimeTool {
    /**
     * @description:获取一年的开始时间和结束时间
     */
    public static Map<Integer, Long[]> getYearTime() {
        Long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        Long time = 0L;
        Map<Integer, Long[]> result = new TreeMap<>();
        Long[] begintrime = new Long[12];
        Long[] endtrime = new Long[12];
        for (Integer i = 0; i < 12; i++) {
            if (i == 0) {
                endtrime[i] = now;
                calendar.add(Calendar.MONTH, -1);
                time = calendar.getTime().getTime();
                begintrime[i] = time;
                continue;
            }
            endtrime[i] = time;
            calendar.add(Calendar.MONTH, -1);
            time = calendar.getTime().getTime();
            begintrime[i] = time;
        }
        result.put(0, begintrime);
        result.put(1, endtrime);
        return result;
    }

    /**
     * @description:获取一月的开始时间和结束时间
     */
    public static Map<Integer, Long[]> getMonthTime() {
        Long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        Integer day = 0;
        Long dayTime = (1000L * 60L * 60L * 24);
        Map<Integer, Long[]> result = new TreeMap<>();
        day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.MONTH, -1);
        Integer cacheday = calendar.get(Calendar.DAY_OF_MONTH);
        //上个月的最后一天
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        day = calendar.get(Calendar.DAY_OF_MONTH) - cacheday + day;
        Long[] begintrime = new Long[day];
        Long[] endtrime = new Long[day];
        for (Integer i = 0; i < day; i++) {
            if (i == 0) {
                endtrime[i] = now;
                begintrime[i] = now - dayTime;
                continue;
            }
            endtrime[i] = now - (dayTime * i);
            begintrime[i] = now - (dayTime * (i + 1));
        }
        result.put(0, begintrime);
        result.put(1, endtrime);
        return result;
    }

    public static Map<Integer, Long[]> getTypeTimeByDateType(String DateType) {
        try {
            if ("year".equals(DateType)) {
                return getYearTime();
            } else if ("month".equals(DateType)) {
                return getMonthTime();
            } else if ("week".equals(DateType)) {
                return getWeekTime();
            } else {//默认一年
                return getYearTime();
            }
        } catch (Exception e) {//默认一年
            return getYearTime();
        }
    }

    /**
     * @description:获取一周的开始时间和结束时间
     */
    public static Map<Integer, Long[]> getWeekTime() {
        Long now = System.currentTimeMillis();
        Long dayTime = (1000L * 60L * 60L * 24);
        Map<Integer, Long[]> result = new TreeMap<>();
        Long[] begintrime = new Long[7];
        Long[] endtrime = new Long[7];
        for (int i = 0; i < 7; i++) {
            if (i == 0) {
                endtrime[i] = now;
                begintrime[i] = now - (dayTime * 1);
                continue;
            }
            endtrime[i] = now - (dayTime * i);
            begintrime[i] = now - dayTime * (i + 1);
        }
        result.put(0, begintrime);
        result.put(1, endtrime);
        return result;
    }

    /**
     * Description:时间转换为时间戳
     * @Return: java.lang.String
    */
    public static String dateToStamp(String s) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String res = "";
        if (!"".equals(s)) {
            try {
                res = String.valueOf(sdf.parse(s).getTime() / 1000);
            } catch (Exception e) {
                try {
                    res = String.valueOf(sdf2.parse(s).getTime() / 1000);
                } catch (Exception e1) {
                    System.out.println("传入了null值");
                }
            }
        } else {
            long time = System.currentTimeMillis();
            res = String.valueOf(time / 1000);
        }

        return res;
    }

    /**
     * @Description: 时间戳转化为 yyyy-MM-dd HH:mm:ss格式
     * @Param: [seconds, format]
     * @return: java.lang.String
    */
    public static String timeStampToDate(Long seconds) {
        if(seconds == null){
            return null;
        }
         String format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(Long.valueOf(seconds+"000")));
    }

    /**
     *@Description: 获取当前时间戳
     * @return: java.lang.long
     */
    public static Long getCurrentTimeStamp(){
        return System.currentTimeMillis()/1000;
    }

    /**
     * @description 获取指定日期所在天/月/年份开始时间戳(默认为月)
     * @param stringTypeDate 标准格式日期
     * @param type 时间类型
     * @return java.lang.long
     */
    public static Long getTimeStampBegin(String stringTypeDate,String type) {
        Date date = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = dateFormat.parse(stringTypeDate);
        } catch (ParseException e) {
            System.out.println("unparseable using " + date);
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if(type.equals("year")){//类型为年则设置月份为1
            c.set(Calendar.MONTH, 0);
        }
        if(type.equals("month")||type.equals("year")){
            //设置为1号,当前日期既为本月第一天
            c.set(Calendar.DAY_OF_MONTH, 1);
        }
        //将小时至0
        c.set(Calendar.HOUR_OF_DAY, 0);
        //将分钟至0
        c.set(Calendar.MINUTE, 0);
        //将秒至0
        c.set(Calendar.SECOND, 0);
        //将毫秒至0
        c.set(Calendar.MILLISECOND, 0);
        // 获取本月第一天的时间戳
        return c.getTimeInMillis();
    }

    /**
     *@Description: 获取指定日期所在天/月/年份结束时间戳(默认为月)
     * @param stringTypeDate 标准格式日期
     * @param type 时间类型
     * @return: java.lang.long
     */
    public static Long getTimeStampEnd(String stringTypeDate,String type) {
        Date date = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = dateFormat.parse(stringTypeDate);
        } catch (ParseException e) {
            System.out.println("unparseable using " + date);
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if(type.equals("year")){//类型为年则设置月份为12
            c.set(Calendar.MONTH,11);
        }
        if(type.equals("month")||type.equals("year")){
            //设置为当月最后一天
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        //将小时至23
        c.set(Calendar.HOUR_OF_DAY, 23);
        //将分钟至59
        c.set(Calendar.MINUTE, 59);
        //将秒至59
        c.set(Calendar.SECOND, 59);
        //将毫秒至999
        c.set(Calendar.MILLISECOND, 999);
        // 获取本月最后一天的时间戳
        return c.getTimeInMillis();
    }

    /**
     * Description:时间转换为13位数的时间戳
     * @Return: java.lang.String
     */
    public static String dateToStamp2(String s) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String res = "";
        if (!"".equals(s)) {
            try {
                res = String.valueOf(sdf.parse(s).getTime() );
            } catch (Exception e) {
                System.out.println("传入了null值");
            }
        } else {
            long time = System.currentTimeMillis();
            res = String.valueOf(time);
        }

        return res;
    }

    /**
     *@Description: 获取当前时刻前N天/月开始和结束时间戳（到毫秒）
     *@return: java.lang.long[]
     */
    public static Long[] getBeginAndEndTime(String dateType,int n){
        Long[] beginAndEndTime = new Long[2];
        long currentTimeMillis = System.currentTimeMillis();
        Long timeEnd = getTimeStampEnd(timeStampToDate(TimeTool.getCurrentTimeStamp()),"day");//今天的结尾
        Long timeBegin = null;
        if(dateType.equals("day")){
            Long dayBegin=getTimeStampBegin(timeStampToDate(TimeTool.getCurrentTimeStamp()),"day");//今天的开头;
            Long dayEnd=timeEnd;
            for(int i=0;i<n;i++){
                dayEnd=timeEnd-Long.valueOf(i+1)*Long.valueOf(24*60*60*1000);
                dayBegin=dayEnd-Long.valueOf(24*60*60*1000);
            }
            beginAndEndTime[0]=dayBegin;
            beginAndEndTime[1]=dayEnd;
        }else {
            timeBegin = getTimeStampEnd(timeStampToDate(TimeTool.getCurrentTimeStamp()), "month")+Long.valueOf(1);//月尾+1等于下个月月头
            for(int i=0;i<=n;i++){
                timeEnd=timeBegin-Long.valueOf(1);//月尾为上个月开头-1
                timeBegin=TimeTool.getTimeStampBegin(TimeTool.timeStampToDate(timeEnd/1000),"month");
            }
            beginAndEndTime[0]=timeBegin;
            beginAndEndTime[1]=timeEnd;
        }
        return beginAndEndTime;
    }

    /**
     *@Description: 获取时间类型的分段数和分段类型
     *@return: java.lang.String[]
     */
    public static String[] getDataPeriodAndType(String dateType){
        if (StringUtils.isEmpty(dateType)) {
            String[] dataPeriodAndType = new String[]{"7","day"};
            return dataPeriodAndType;
        }
        int dataPeriod=7;//默认为最近七天，有七段数据
        String dataPeriodType="day";//时间跨度类型 默认为天
        if(dateType.equals("month")){
            dataPeriod=30;
        }else if(dateType.equals("year")){
            dataPeriod=12;
            dataPeriodType="month";//统计年时时间跨度为月
        }
        String[] dataPeriodAndType = new String[]{String.valueOf(dataPeriod),dataPeriodType};
        return dataPeriodAndType;
    }



    /**
     * @Description:根据时间类型返回一段时间
     * @Param [dateType]
     * @Return com.gongfeng.workbeeprj.common.entity.devicesystem.DeviceStateStatistics
     */
    public static List<String> getDateListByDateType(String dateType){
        List<String> dateList=new ArrayList<>();
        int dataPeriod = 7;
        String dataPeriodType="day";
        if(dateType.equals("month")){
            dataPeriod=30;
        }else if(dateType.equals("year")){
            dataPeriod=12;
            dataPeriodType="month";
        }
        for(int i=0;i<dataPeriod;i++){
            if(dateType.equals("month")||dateType.equals("week")){
                dateList.add(timeStampToDate(getBeginAndEndTime(dataPeriodType,i)[1]/1000).substring(5,10));
            }else {
                String substring = timeStampToDate(getBeginAndEndTime(dataPeriodType, i)[1] / 1000).substring(5, 7);
                if(substring.substring(0,1).equals("0")){
                    substring=substring.substring(1,2);
                }
                dateList.add(substring);
            }
        }
        return dateList;
    }
}
