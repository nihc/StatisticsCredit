package com.cardpay.pccredit.bank.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cardpay.pccredit.bank.model.BankDataFileProcess;
import com.cardpay.pccredit.bank.util.BankFtpConfig;
import com.cardpay.pccredit.bank.util.CardFtpUtils;
import com.cardpay.pccredit.bank.util.DateUtils;
import com.cardpay.pccredit.bank.util.ExpandGZ;
import com.cardpay.sftp.Sftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.sun.org.apache.bcel.internal.generic.NEW;

/**
 * @author chenzhifang
 *
 * 2014-12-8下午3:35:55
 */
@Service
public class DownMakeCardReturnSchedulesService {
	public Logger log = Logger.getLogger(DownMakeCardReturnSchedulesService.class);
	
	public static final String PARSE = "1";
	
	public static final String FAIL = "2";
	
	public String curRemotePath = "";//保存本次remotePath
	
	@Autowired
	private BankFtpConfig bankFtpConfig;
	
	@Autowired
	private BankDataFileProcessService bankDataFileProcessService;
	
	@Autowired
	private SXykAcctCurService sXykAcctCurService;
	
	@Autowired
	private SXykCardCurService sXykCardCurService;
	
	@Autowired
	private XmAccCreditService xmAccCreditService;
	
	@Autowired
	private SXykStmtCurService sXykStmtCurService;
	
	@Autowired
	private SXykCustrCurService sXykCustrCurService;
	
	@Autowired
	private AgrCrdXykCunegService agrCrdXykCunegService;
	
	//public static boolean flag = true;
	public void init() {
		File file = new File(bankFtpConfig.getDownloadPath());
		// 检测下载临时保存目录是否存在
		if(!file.exists()){
			file.mkdirs();
		}
		// 检测备份目录是否存在
		file = new File(bankFtpConfig.getBackupPath());
		if(!file.exists()){
			file.mkdirs();
		}
	}
	
	/**
	 * 下载并解析制卡结果文件定时任务
	 * 每天22：05取文件
	 * @return
	 * @throws Exception 
	 */
	@Scheduled(cron = "0 05 22 * * ?")
	//@Scheduled(fixedDelay=5000)
	public void downloadMakeCardAndParseDataSchedules(){
		System.out.println("下载文件：");
		CardFtpUtils sftp = new CardFtpUtils();
		try {
			sftp.connect();
			curRemotePath = CardFtpUtils.bank_ftp_path;
			log.info("downloadMakeCardAndParseDataSchedules path = "+ curRemotePath);
			//获取文件列表
			ArrayList<String> files = sftp.getList(curRemotePath);
			// 处理ftp文件
			processFtpFile(sftp, files);
			
		} catch (JSchException e) {
			log.error("", e);
		} catch (SftpException e) {
			log.error("", e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(sftp != null){
				sftp.close();
				System.out.println("success,,/////");
			}
		}
		//flag = false;
	}
	
	/**
	 * 处理ftp文件
	 * @return
	 * @throws Exception 
	 */
	public void processFtpFile(CardFtpUtils sftp, ArrayList<String> files) throws Exception{
		Iterator<String> pathIterator = files.iterator();
		// 下载路径
		String downloadPath =  CardFtpUtils.bank_ftp_down_path;
		downloadPath = URLDecoder.decode(downloadPath, "utf-8");
		
		//获取今日日期（实际是昨天的制卡文件）
      //yyyyMMdd格式
		DateFormat format = new SimpleDateFormat("yyyyMMdd");
		String dateString = format.format(new Date());
		while(pathIterator.hasNext()){
			String file = pathIterator.next();
			try{
				if(file.indexOf(dateString)>-1){
					//下载文件
					if(sftp.download(curRemotePath, file, downloadPath)){
						log.error("下载文件" + file + "成功");
					}else{
						log.error("下载文件" + file + "失败");
					}
					
				}
			}catch(Exception e){
				log.error("处理文件" + file + "出错", e);
			}
		}
	}
	
}
