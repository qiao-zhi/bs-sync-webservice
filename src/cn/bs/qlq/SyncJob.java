package cn.bs.qlq;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bs.qlq.utils.JDBCUtils;
import cn.bs.qlq.utils.ResourcesUtil;
import cn.infohow.mes.ei.remote.ChxOutsideDataRemoteService;
import cn.infohow.mes.ei.remote.ChxOutsideDataRemoteServiceService;
import cn.infohow.mes.ei.remote.ManuOrderDetailInfo;
import cn.infohow.mes.ei.remote.VinGenerateInfo;

public class SyncJob {

	private static final ScheduledExecutorService batchTaskPool = Executors.newScheduledThreadPool(1);

	private static final Logger log = LoggerFactory.getLogger(SyncJob.class);

	private static String defaultQueryVinSQL = "select count(*) from VINLIST where PRDSEQ = ?";
	private static final String queryVinSQL = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "queryVin"),
			defaultQueryVinSQL);
	private static String defaultInsertVinSQL = "insert into vinlist (FCCODE, MONNUN, MTCODE, PLCODE, PRDDATE, PRDSEQ, PRIORITY, SUCCFLG, SUNROOF, VININDEX) values (?,?,?,?,?,?,?,?,?,?)";
	private static final String insertVinSQL = StringUtils
			.defaultIfBlank(ResourcesUtil.getValue("sync", "insertVinSQL"), defaultInsertVinSQL);

	private static String defaultQueryOrderSQL = "select count(*) from ORDERLIST where MONUM = ?";
	private static final String queryOrderSQL = StringUtils
			.defaultIfBlank(ResourcesUtil.getValue("sync", "queryOrderSQL"), defaultQueryOrderSQL);
	private static String defaultInsertOrderSQL = "insert into ORDERLIST (MONUM, MOTYPE, MTCODE, PLCODE, CARTYPE, PRIORITY, PROSCHEDULDATE, PROSCHEDULNUM, SPECIALFLG) values (?,?,?,?,?,?,?,?,?)";
	private static final String insertOrderSQL = StringUtils
			.defaultIfBlank(ResourcesUtil.getValue("sync", "insertOrderSQL"), defaultInsertOrderSQL);

	public static void main(String[] args) throws ParseException {
		syncVinQueue();
		syncOrderList();

		long curDateSecneds = 0;
		try {
			String time = "23:00:00";
			DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
			DateFormat dayFormat = new SimpleDateFormat("yy-MM-dd");
			Date curDate = dateFormat.parse(dayFormat.format(new Date()) + " " + time);
			curDateSecneds = curDate.getTime();
		} catch (ParseException ignored) {
			// ignored
		}

		long initialDelay = (curDateSecneds - System.currentTimeMillis()) / 1000;
		batchTaskPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				syncVinQueue();
				syncOrderList();
			}
		}, initialDelay, 1, TimeUnit.DAYS);
	}

	/**
	 * 同步 vin队列
	 */
	private static void syncVinQueue() {
		String currentData = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
		String startDate = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "vinQueueStartDate"), currentData);
		String endDate = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "vinQueueEndDate"), "");

		ChxOutsideDataRemoteServiceService chxOutsideDataRemoteServiceService = new ChxOutsideDataRemoteServiceService();
		ChxOutsideDataRemoteService chxOutsideDataRemoteService = chxOutsideDataRemoteServiceService
				.getChxOutsideDataRemoteServicePort();
		List<VinGenerateInfo> vinGenerateInfos = chxOutsideDataRemoteService.selectVinListForService(startDate,
				endDate);

		if (CollectionUtils.isNotEmpty(vinGenerateInfos)) {
			saveVins(vinGenerateInfos);

			log.info("vins 数据已保存");
			return;
		}

		log.error("vins 没有数据");
	}

	private static void saveVins(List<VinGenerateInfo> vinGenerateInfos) {
		for (VinGenerateInfo info : vinGenerateInfos) {
			String preseq = StringUtils.defaultString(info.getPrdseq(), "");

			if (StringUtils.isBlank(preseq)) {
				log.error("没有 preseq -> {}, 跳过", preseq);
				continue;
			}

			Object result = JDBCUtils.executeSQL(queryVinSQL, preseq);
			if (result != null && Integer.valueOf(result.toString()) > 0) {
				log.error("存在相同的数据, preseq -> {}, 跳过", preseq);
				continue;
			}

			String fccode = StringUtils.defaultString(info.getFccode(), "");
			String monnum = StringUtils.defaultString(info.getMonnum(), "");
			String mtcode = StringUtils.defaultString(info.getMtcode(), "");
			String plcode = StringUtils.defaultString(info.getPlcode(), "");
			String predate = StringUtils.defaultString(info.getPrddate(), "");

			int priority = info.getPriority();
			String succflag = StringUtils.defaultString(info.getSuccflg(), "");
			String sunroof = StringUtils.defaultString(info.getSunroof(), "");
			String vinindex = StringUtils.defaultString(info.getVinindex(), "");

			log.debug("参数: {}, {}, {}, {}, {}, {}, {}, {}, {}, {}", fccode, monnum, mtcode, plcode, predate, preseq,
					priority, succflag, sunroof, vinindex);
			JDBCUtils.executeSQL(insertVinSQL, fccode, monnum, mtcode, plcode, predate, preseq, priority, succflag,
					sunroof, vinindex);
		}
	}

	private static void syncOrderList() {
		String currentData = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
		String startDate = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "orderListStartDate"),
				currentData);
		String endDate = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "orderListSEndDate"), "");
		String intDay = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "orderListIntDay"), "1");
		String flowLine = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "orderListFlowLine"), "xtmq0101");

		ChxOutsideDataRemoteServiceService chxOutsideDataRemoteServiceService = new ChxOutsideDataRemoteServiceService();
		ChxOutsideDataRemoteService chxOutsideDataRemoteService = chxOutsideDataRemoteServiceService
				.getChxOutsideDataRemoteServicePort();
		List<ManuOrderDetailInfo> orders = chxOutsideDataRemoteService.selectManuOrderDetilListService(startDate,
				endDate, intDay, flowLine);

		if (CollectionUtils.isNotEmpty(orders)) {
			saveOrders(orders);

			log.debug("orders 数据已保存");
			return;
		}

		log.error("orders 没有数据");
	}

	private static void saveOrders(List<ManuOrderDetailInfo> orders) {
		for (ManuOrderDetailInfo order : orders) {
			String monum = StringUtils.defaultString(order.getMonum(), "");

			if (StringUtils.isBlank(monum)) {
				log.error("没有 MONUM -> {}, 跳过");
				continue;
			}

			Object result = JDBCUtils.executeSQL(queryOrderSQL, monum);
			if (result != null && Integer.valueOf(result.toString()) > 0) {
				log.error("存在相同的数据, monum -> {}, 跳过", monum);
				continue;
			}

			String motype = StringUtils.defaultString(order.getMotype(), "");
			String mtcode = StringUtils.defaultString(order.getMtcode(), "");
			String plcode = StringUtils.defaultString(order.getPlcode(), "");
			String cartype = StringUtils.defaultString(order.getCartype(), "");
			String priority = order.getPriority();
			String proscheduldate = StringUtils.defaultString(order.getProscheduldate(), "");
			String proschedulnum = StringUtils.defaultString(order.getProschedulnum(), "");
			String specialflag = StringUtils.defaultString(order.getSpecialflag(), "");
			if ("0".equals(specialflag)) {
				specialflag = "否";
			} else if ("1".equals(specialflag)) {
				specialflag = "是";
			}

			log.debug("参数: {}, {}, {}, {}, {}, {}, {}, {}, {}", monum, motype, mtcode, plcode, cartype, priority,
					proscheduldate, proschedulnum, specialflag);
			JDBCUtils.executeSQL(insertOrderSQL, monum, motype, mtcode, plcode, cartype, priority, proscheduldate,
					proschedulnum, specialflag);
		}
	}
}
