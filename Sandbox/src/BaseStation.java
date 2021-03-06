import com.virtenio.preon32.examples.common.USARTConstants;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

import java.io.OutputStream;
import com.virtenio.driver.usart.NativeUSART;
import com.virtenio.driver.usart.USART;
import com.virtenio.driver.usart.USARTException;
import com.virtenio.driver.usart.USARTParams;

public class BaseStation extends Thread{
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] nodes = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	private static int SELF_ADDR = nodes[0]; // NODE DIRINYA (BS)
	private static int CH_ADDR[] = {nodes[1],nodes[3]};
//=================================================================================================
	private static USART usart;
	private static OutputStream out;
	private static boolean stop;
	private static String modeAgr;
	private static int agr = CH_ADDR.length;

	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, SELF_ADDR, SELF_ADDR, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);
			Thread thread = new Thread() {
				public void run() {
					try {
						receive(fio);
						sender(fio);
					} catch (Exception e) {
					}
				}
			};
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sender(final FrameIO fio) throws Exception {
		while (true) {
			int temp = 1000;
			try {
				temp = usart.read();
			} 
			catch (USARTException e1) {
				e1.printStackTrace();
			}
			if (temp == 0) {
				try {
					for(int i=0;i<CH_ADDR.length;i++) {
						send("STOP", CH_ADDR[i], fio);
						Thread.sleep(10);
					}
				} 
				catch (Exception e1) {
					e1.printStackTrace();
				}
			} 
			else if (temp == 1) {
				stop = false;
				long currTime = Time.currentTimeMillis();
				try {
					for (int i=0;i<CH_ADDR.length;i++) {
						send(("t" + currTime), CH_ADDR[i], fio);
						Thread.sleep(10);
					}
				} 
				catch (Exception e1) {
				}
			}
			else if (temp == 2) {
				stop = false;
				try {
					for(int i=0;i<CH_ADDR.length;i++) {
						send("ON", CH_ADDR[i], fio);
						Thread.sleep(10);
					}
				} 
				 catch (Exception e1) {
					e1.printStackTrace();
				}
			} 
			else if (temp == 3) {
				stop = false;
				try {
					for(int i=0;i<CH_ADDR.length;i++) {
						send("DETECT", CH_ADDR[i], fio);
						Thread.sleep(10);
					}
					
				} 
				catch (Exception e1) {
					e1.printStackTrace();
				}
				
			}
			else if(temp == 4) {
				stop = false;
				try {
					for(int i=0;i<CH_ADDR.length;i++) {
						modeAgr = "AGGR1";
						send("AGGR1", CH_ADDR[i], fio);
						Thread.sleep(10);
					}
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			else if(temp == 5) {
				stop = false;
				try {
					for(int i=0;i<CH_ADDR.length;i++) {
						modeAgr = "AGGR2";
						send("AGGR2", CH_ADDR[i], fio);
						Thread.sleep(10);
					}
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			else if(temp == 6) {
				stop = false;
				try {
					for(int i=0;i<CH_ADDR.length;i++) {
						modeAgr = "AGGR3";
						send("AGGR3", CH_ADDR[i], fio);
						Thread.sleep(10);
					}
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}
				
			}
		}
	}

	public static void receive(final FrameIO fio) throws Exception {
		Thread receive = new Thread() {
			public void run() {
				Frame frame = new Frame();
				while (true) {
					try {
						fio.receive(frame);
						byte[] isi = frame.getPayload();
						String str = new String(isi, 0, isi.length);
						// DAPAT NODE YANG ONLINE
						if (str.charAt(0) == 'N') {
							String msg = "#" +str+"#";
							try {
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						else if (str.charAt(0)=='T') {
							String msg = "#"+str+"#";
							try {
								out.write(msg.getBytes(),0,msg.length());
								usart.flush();
							}
							catch (Exception e){
								
							}
						}
						else if(str.charAt(str.length()-1)=='D'&&stop==false) {
							try {
								out.write(str.getBytes(),0,str.length());
								stop = true;
							}
							catch(Exception e) {
								
							}
						}
						else if (str.charAt(0) == 'S'&&stop!=true) {
							String msg = "#" + str + "#";
							try {
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} 
							catch (Exception e) {
								
							}
						}
						else if(str.charAt(0)=='A'&&stop!=true) {
							String msg = "#" + str + "#";
							try {
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
								
							}
							catch (Exception e) {
								 
							}
						}
						else if(str.charAt(0)=='D') {
							agr-=1;
							if(agr==0) {
								for(int i=0;i<CH_ADDR.length;i++) {
									send(modeAgr, CH_ADDR[i], fio);
									Thread.sleep(10);
								}
								agr = CH_ADDR.length;
							}
						}
					} 
					catch (Exception e) {
						
					}
				}
			}
		};
		receive.start();
	}

	public static void send(String msg, long address, final FrameIO fio) throws Exception {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.ACK_REQUEST
				| Frame.SRC_ADDR_16;
		final Frame testFrame = new Frame(frameControl);
		testFrame.setDestPanId(COMMON_PANID);
		testFrame.setDestAddr(address);
		testFrame.setSrcAddr(SELF_ADDR);
		testFrame.setPayload(msg.getBytes());
		try {
			fio.transmit(testFrame);
			Thread.sleep(50);
		} catch (Exception e) {
		}
	}

	private static USART configUSART() {
		USARTParams params = USARTConstants.PARAMS_115200;
		NativeUSART usart = NativeUSART.getInstance(0);
		try {
			usart.close();
			usart.open(params);
			return usart;
		} catch (Exception e) {
			return null;
		}
	}

	private static void startUSART() {
		usart = configUSART();
	}
	
	

	public static void main(String[] args) {
		try {
			startUSART();
			out = usart.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		runs();

	}

}
