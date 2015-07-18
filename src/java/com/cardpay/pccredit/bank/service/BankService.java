/**
 * 
 */
package com.cardpay.pccredit.bank.service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cardpay.pccredit.bank.constant.Constants;
import com.cardpay.pccredit.bank.dao.BankDao;
import com.cardpay.pccredit.bank.dao.CustomerAccountBillDao;
import com.cardpay.pccredit.bank.dao.CustomerCardInformationDao;
import com.cardpay.pccredit.bank.dao.SXykStmtCurDao;
import com.cardpay.pccredit.bank.id.IDGenerator;
import com.cardpay.pccredit.bank.model.AverageDailyOverdraft;
import com.cardpay.pccredit.bank.model.CardInformation;
import com.cardpay.pccredit.bank.model.CustomerAccountBill;
import com.cardpay.pccredit.bank.model.CustomerCardInformation;
import com.cardpay.pccredit.bank.model.SXykAcctCur;
import com.cardpay.pccredit.bank.model.SXykCardCur;
import com.cardpay.pccredit.bank.model.SXykStmtCur;
import com.cardpay.pccredit.bank.util.AmountConfig;
import com.cardpay.pccredit.bank.util.Arith;
import com.cardpay.pccredit.datasource.DataSourceContextHolder;
import com.wicresoft.util.date.DateHelper;

/**
 * @author shaoming
 *
 * 2014年12月9日   下午4:31:58
 */
@Service
public class BankService extends AmountConfig{
	/*日志*/
	Logger logger = Logger.getLogger(this.getClass());
	@Autowired
	private SXykStmtCurDao sXykStmtCurDao;

	@Autowired
	private BankDao bankDao;

	@Autowired
	private CustomerCardInformationDao customerCardInformationDao;

	@Autowired
	private CustomerAccountBillDao customerAccountBillDao;

	@Autowired
	private SXykCardCurService sXykCardCurService;

	@Autowired
	private SXykAcctCurService sXykAcctCurService;
	/**
	 * 银行接口对接,每月同步客户卡片信息(费用)和客户账户账单
	 */
	@Scheduled(cron = "${b2b.link.scheduled.month}")
	public void addSXykStmtCurSync(){
		List<SXykStmtCur> SXykStmtCurs = sXykStmtCurDao.findSXykStmtCur();
		/*客户账户账单*/
		CustomerAccountBill bill = null;
		/*客户卡片信息*/
		CustomerCardInformation card = null;
		boolean flag = true;
		for(SXykStmtCur sxsc : SXykStmtCurs){
			/*卡号*/
			String cardNbr = sxsc.getCardNbr();
			/*账单日*/
			String cycleNbr = sxsc.getCycleNbr()==null?null:String.valueOf(sxsc.getCycleNbr());
			if(StringUtils.isNotEmpty(cycleNbr)){
				/*利息合计(应收利息)*/
				String balOrint = sxsc.getBalOrint()==null?null:String.valueOf(sxsc.getBalOrint());
				if(StringUtils.isEmpty(balOrint)){
					logger.error("卡号为"+cardNbr+"账单记录 利息合计BAL_ORINT字段不存在数据");
					balOrint = initialAmount;
				}
				/*当前未计收的利息*/
				String intCunot = sxsc.getIntCunot()==null?null:String.valueOf(sxsc.getIntCunot());;
				if(StringUtils.isEmpty(intCunot)){
					logger.error("卡号为"+cardNbr+"账单记录 当前未计收的利息int_Cunot字段不存在数据");
					intCunot = initialAmount;
				}
				/*利息收入 = 利息合计(应收利息)-当前未计收的利息*/
				String interest = Arith.subReturnStr(balOrint, intCunot);
				/*账号*/
				String xaccount = sxsc.getXaccount()==null?null:String.valueOf(sxsc.getXaccount());
				if(StringUtils.isNotEmpty(xaccount)){
					/*添加客户账户账单*/
					bill = new CustomerAccountBill();
					bill.setAccountNumber(xaccount);//修改目的。
					bill.setCardNumber(cardNbr);
					/*已发生逾期金额（逾期金额）*/
					String overdueAmount = sxsc.getOdueHeld()==null?null:String.valueOf(sxsc.getOdueHeld());
					if(StringUtils.isEmpty(overdueAmount)){
						logger.error("账号为"+xaccount+"的账单记录 已发生逾期金额（逾期金额）Odue_Held字段数据不存在");
						overdueAmount=initialAmount;
					}
					overdueAmount = String.valueOf(Arith.mul(overdueAmount, multiple));
					bill.setOverdueAmount(overdueAmount.substring(0,overdueAmount.indexOf(".")));
					/*当月的利息金额*/
					String currentMonthInterestAccount = sxsc.getIntEarned()==null?null:String.valueOf(sxsc.getIntEarned());
					if(StringUtils.isEmpty(currentMonthInterestAccount)){
						logger.error("账号为"+xaccount+"的账单记录 当月的利息金额 int_earned字段数据不存在");
						currentMonthInterestAccount=initialAmount;
					}
					currentMonthInterestAccount = String.valueOf(Arith.mul(currentMonthInterestAccount,multiple));
					bill.setCurrentMonthInterestAccount(currentMonthInterestAccount.substring(0,currentMonthInterestAccount.indexOf(".")));
					/*还款金额*/
					String paybackAccount = sxsc.getPayment()==null?null:String.valueOf(sxsc.getPayment());
					/*通过卡号得到账户资料信息*/
					SXykAcctCur sxac = sXykAcctCurService.findSXykAcctCurByCardNbr(cardNbr);
					/*得到最近一期帐单的最低应缴款STM_MINDUE*/
					String stmMindue = sxac.getStmMindue()==null?null:String.valueOf(sxac.getStmMindue());
					if(DataSourceContextHolder.getDbType()==null || !DataSourceContextHolder.getDbType().equals(DataSourceContextHolder.PCCREDIT)){
						DataSourceContextHolder.setDbType(DataSourceContextHolder.PCCREDIT);
					}
					/*判断还款金额与最低应缴款是否相等*/
					if(StringUtils.isNotEmpty(paybackAccount)){
						if(StringUtils.isNotEmpty(stmMindue)){
							if(paybackAccount.equalsIgnoreCase(stmMindue)){
								/*得到账户账号*/
								String account = sxac.getXaccount();
								/*得到账户账号得到当月客户经理日均透支额度*/
								Calendar cal=Calendar.getInstance();
								AverageDailyOverdraft average = bankDao.findAverageDailyOverdraftByAccountNumber(account,cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1);
								/*设置最低还款标志*/
								if(average!=null){
									average.setLowDueStatus(Constants.LOWDUESTATUS);
								}
								/*更新客户经理日均透支额度*/
								bankDao.updateAverageDailyOverdraft(average);
							}
						}else{
							logger.error("卡号:"+cardNbr+"的账户资料信息，最低应缴款STM_MINDUE不存在");
						}
					}
					if(StringUtils.isEmpty(paybackAccount)){
						logger.error("账号为"+xaccount+"的账单记录 还款金额 payment字段数据不存在");
						paybackAccount=initialAmount;
					}
					paybackAccount = String.valueOf(Arith.mul(paybackAccount, multiple));
					bill.setPaybackAccount(paybackAccount.substring(0,paybackAccount.indexOf(".")));
					/*透支余额 */
					String overdraftAccount = sxsc.getCloseBal()==null?null:String.valueOf(sxsc.getCloseBal());
					if(StringUtils.isEmpty(overdraftAccount)){
						logger.error("账号为"+xaccount+"的账单记录 透支余额 close_Bal字段数据不存在");
						overdraftAccount=initialAmount;
					}
					overdraftAccount = String.valueOf(Arith.mul(overdraftAccount, multiple));
					bill.setOverdraftAccount(overdraftAccount.substring(0,overdraftAccount.indexOf(".")));
					/*实收利息=利息收入*/
					interest = String.valueOf(Arith.mul(interest, multiple));
					bill.setPaidInterestAccount(interest.substring(0,interest.indexOf(".")));
					try{
						flag = insertCustomerAccountBill(bill,cycleNbr);
						if(!flag){
							logger.error("添加客户账户账单失败");
							flag = true;
						}
					}catch(Exception e){
						logger.error("添加客户账户账单异常"+e.getMessage());
						e.printStackTrace();
					}
				}else{
					logger.error("卡号为"+cardNbr+"的账单记录 账号不存在");
				}			

				/*添加客户卡片信息 CustomerCardInformation*/
				/*通过卡号得到卡片信息的id(卡片id)*/
				CardInformation cardInfor = bankDao.findCardInformation(cardNbr);
				if(cardInfor!=null){
					String cardId = cardInfor.getId();
					SXykAcctCur sxac = sXykAcctCurService.findSXykAcctCurByCardNbr(cardNbr);
					/*卡片id存在*/
					if(cardId!=null && StringUtils.isNotEmpty(cardId)){
						card = new CustomerCardInformation();
						/*卡片激活处理*/
						if(sxac!=null){
							/*核销标志*/
							String wrofFlag = sxac.getWrofFlag();
							if(StringUtils.isNotEmpty(wrofFlag)){
								/*核销标志为1,则表示为非激活卡*/
								if(wrofFlag.equalsIgnoreCase(Constants.Verification)){
									card.setActivationStatus(Constants.NonActiveCard);
								}else{
									/*核销标志不为1时，判断激活时间在当前时间前,则表示为激活卡,否则为非激活卡*/
									SXykCardCur sxcc = sXykCardCurService.findSXykCardCurByCardNbr(cardNbr);
									if(sxcc!=null){
										/*得到卡片激活时间*/
										Date activeday = sxcc.getActiveday()==null?null:DateHelper.getDateFormat(sxcc.getActiveday(), "yyyyMMdd");
										if(activeday!=null){
											/*得到当前时间*/
											Date nowTime = DateHelper.normalizeDate(new Date(), "yyyyMMdd");
											if(activeday.getTime()<=nowTime.getTime()){
												card.setActivationStatus(Constants.ActiveCard);
											}else{
												logger.error("卡号为"+cardNbr+"的卡片资料信息的卡片激活时间:"+DateHelper.getDateFormat(activeday, "yyyyMMdd")+"大于现在时间:"+DateHelper.getDateFormat(nowTime, "yyyyMMdd"));
											}
										}else{
											logger.error("卡号为"+cardNbr+"的卡片资料信息的卡片激活时间不存在");
										}
									}else{
										logger.error("卡号为"+cardNbr+"的卡片资料信息不存在");
									}
								}
							}else{
								logger.error("卡号为"+cardNbr+"的账户资料表,核销标志wrof_flag不存在");
							}
						}
						card.setCardId(cardId);
						/*还款金额*/
						BigDecimal payment = sxsc.getPayment()==null?new BigDecimal(initialAmount):sxsc.getPayment();
						if(payment.compareTo(BigDecimal.ZERO)>0){
							card.setActiveStatus("0");
						}
						/*年费*/
						String cardFees = sxsc.getCardFees()==null?null:String.valueOf(sxsc.getCardFees());
						if(StringUtils.isEmpty(cardFees)){
							logger.error("卡号为"+cardNbr+"账单记录 年费Card_Fees字段不存在数据");
							cardFees = initialAmount;
						}
						cardFees = String.valueOf(Arith.mul(cardFees, multiple));
						card.setAnnualFee(cardFees.substring(0,cardFees.indexOf(".")));
						/*利息收入*/
						interest = String.valueOf(Arith.mul(interest, multiple));
						card.setInterest(interest.substring(0,interest.indexOf(".")));
						try{
							flag = insertCustomerCardInformation(card,cycleNbr);
							if(!flag){
								logger.error("添加客户卡片信息失败");
								flag = true;
							}
						}catch(Exception e){
							logger.error("添加客户卡片信息异常"+e.getMessage());
							continue;
						}
						if(DataSourceContextHolder.getDbType()==null || !DataSourceContextHolder.getDbType().equalsIgnoreCase(DataSourceContextHolder.BANK)){
							DataSourceContextHolder.setDbType(DataSourceContextHolder.BANK);
						}
						SXykCardCur sxcc = sXykCardCurService.findSXykCardCurByCardNbr(cardNbr);
						if(sxcc!=null){
							String canclCode = sxcc.getCanclCode();
							if(StringUtils.isNotEmpty(canclCode)){
								if(!canclCode.equalsIgnoreCase("0")){
									canclCode="1";
								}
								/*通过卡号得到卡片信息的id(卡片id)*/
								if(cardInfor!=null){
									if(cardId!=null && StringUtils.isNotEmpty(cardId)){
										/*有效卡*/
										card.setEffectiveStatus(canclCode);
										/*更新*/
										try{
											if(DataSourceContextHolder.getDbType()==null || !DataSourceContextHolder.getDbType().equalsIgnoreCase(DataSourceContextHolder.PCCREDIT)){
												DataSourceContextHolder.setDbType(DataSourceContextHolder.PCCREDIT);
											}
											/*更新客户卡片信息*/
											updateByPrimaryKeySelective(card);
										}catch(Exception e){
											logger.error("客户卡片信息更新异常!"+e.getMessage());
										}
									}
								}
							}
						}
					}else{
						logger.error("卡号:"+cardNbr+"相对应卡片信息id不存在");
					}
				}else{
					logger.error("卡号:"+cardNbr+"相对应卡片信息不存在");
				}
			}else{
				logger.error("卡号为"+cardNbr+"的账单记录,账单日不存在");
				continue;
			}
		}
	}
	/*添加客户账户账单*/
	private boolean insertCustomerAccountBill(CustomerAccountBill bill,String cycleNbr){
		HashMap<String,Integer> map = getYearAndMonth(cycleNbr);
		bill.setId(IDGenerator.generateID());
		bill.setCreatedDate(DateHelper.normalizeDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		bill.setBillDataMonth(BigDecimal.valueOf(map.get("month")));
		bill.setBillDataYear(BigDecimal.valueOf(map.get("year")));
		int i = customerAccountBillDao.insertSelective(bill);
		return i>0?true:false;
	}
	/*添加客户卡片信息*/
	public boolean insertCustomerCardInformation(CustomerCardInformation card,String cycleNbr){
		HashMap<String,Integer> map = getYearAndMonth(cycleNbr);
		card.setId(IDGenerator.generateID());
		card.setYear(String.valueOf(map.get("year")));
		card.setMonth(String.valueOf(map.get("month")));
		card.setCreatedTime(DateHelper.normalizeDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		int i = customerCardInformationDao.insertSelective(card);
		return i>0?true:false;
	}
	/*得到本月月份与对应年份*/
	private HashMap<String,Integer> getYearAndMonth(String cycleNbr){
		Calendar cal=Calendar.getInstance();
		//		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		/*字段 Month*/
		int month = cal.get(Calendar.MONTH)+1;;
		/*字段 Year*/
		int year = cal.get(Calendar.YEAR);
		//		/*当日大于账单日,月份为下月*/
		//		if(dayOfMonth>Integer.valueOf(cycleNbr)){
		//			month += 1;
		//			/*例:12月 下月为次年1月*/
		//			if(month>12){
		//				year += 1;
		//				month -= 12;
		//			}
		//		}
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		map.put("month", month);
		map.put("year", year);
		return map;
	}
	private boolean updateByPrimaryKeySelective(CustomerCardInformation record){
		record.setModifiedTime(DateHelper.normalizeDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		int i = customerCardInformationDao.updateByPrimaryKeySelective(record);
		return i>0?true:false;
	}
}
