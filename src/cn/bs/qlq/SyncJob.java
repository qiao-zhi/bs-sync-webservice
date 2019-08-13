package cn.bs.qlq;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import cn.bs.qlq.utils.BeanUtils;
import cn.bs.qlq.utils.ResourcesUtil;
import cn.infohow.mes.ei.remote.ChxOutsideDataRemoteService;
import cn.infohow.mes.ei.remote.ChxOutsideDataRemoteServiceService;
import cn.infohow.mes.ei.remote.ManuOrderDetailInfo;
import cn.infohow.mes.ei.remote.VinGenerateInfo;

public class SyncJob {

	public static void main(String[] args) {
		syncVinQueue();

		syncOrderList();
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
		List<VinGenerateInfo> selectVinListForService = chxOutsideDataRemoteService.selectVinListForService(startDate,
				endDate);

		if (CollectionUtils.isNotEmpty(selectVinListForService)) {
			try {
				List<Map<String, Object>> beansToMaps = BeanUtils.beansToMaps(selectVinListForService, true, true);
				FileUtils.writeStringToFile(new File("result.txt"), beansToMaps.toString(), "UTF-8", true);
			} catch (Exception ignored) {
				// ignored
			}

			System.out.println("vins 数据已保存");
			return;
		}

		System.out.println("vins 没有数据");
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
			try {
				List<Map<String, Object>> beansToMaps = BeanUtils.beansToMaps(orders, true, true);
				FileUtils.writeStringToFile(new File("result2.txt"), beansToMaps.toString(), "UTF-8", true);
			} catch (Exception ignored) {
				// ignored
			}

			System.out.println("orders 数据已保存");
			return;
		}

		System.out.println("orders 没有数据");
	}
}
