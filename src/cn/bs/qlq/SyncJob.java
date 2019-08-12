package cn.bs.qlq;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import cn.bs.qlq.utils.ResourcesUtil;

public class SyncJob {

	public static void main(String[] args) {
		syncVinQueue();
	}

	/**
	 * 同步 vin队列
	 */
	private static void syncVinQueue() {
		JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();

		String vinQueueWSDLUrl = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "vinQueueWSDLUrl"),
				"http://10.98.100.160:7001/ghis/remote/chxOutsideData/ChxOutsideDataRemoteService?wsdl");
		String vinQueueNameSpaceUrl = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "vinQueueNameSpaceUrl"),
				"http://WebXml.com.cn/");
		String vinQueueMethodName = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "vinQueueNameSpaceUrl"),
				"selectVinListForService");

		// url为调用webService的wsdl地址
		Client client = dcf.createClient(vinQueueWSDLUrl);
		// namespace是命名空间，methodName是方法名
		QName name = new QName(vinQueueNameSpaceUrl, vinQueueMethodName);

		String currentData = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
		String startDate = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "vinQueueStartDate"), currentData);
		String endDate = StringUtils.defaultIfBlank(ResourcesUtil.getValue("sync", "vinQueueStartDate"), "");

		Object[] objects;
		try {
			objects = client.invoke(name, startDate, endDate);// 第一个参数是上面的QName，第二个开始为参数，可变数组
			System.out.println(Arrays.toString(objects));
			FileUtils.writeStringToFile(new File("result.txt"), Arrays.toString(objects), "UTF-8", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
