import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;
import com.virtenio.misc.PropertyHelper;

import java.util.LinkedList;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

public class ClusterHead {
	private AccelerationSensor AS = new AccelerationSensor();
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] nodes = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	// =======================================================================================================
	private static int BASE_ADDR = nodes[0];
	
//	private static int LEAF_ADDR[] = {nodes[4],nodes[5]};
//	private static int SELF_ADDR = nodes[3];
//	
	private static int LEAF_ADDR[] = {nodes[2]};
	private static int SELF_ADDR = nodes[1];
	

	private int timeAggr = 0;
	private long timeStart =0;
	private boolean stop = false;
	private LinkedList<String> tempX = new LinkedList<String>();
    private LinkedList<String> tempY = new LinkedList<String>();
    private LinkedList<String> tempZ = new LinkedList<String>();
    private String senseX, senseY, senseZ;
	private int[] senseAgrX = new int[LEAF_ADDR.length];
	private int[] senseAgrY= new int[LEAF_ADDR.length];
	private int[] senseAgrZ = new int[LEAF_ADDR.length];
	private PCA pcX, pcY, pcZ;
	private String[] tempSensing = new String[LEAF_ADDR.length];
	private boolean[] agr = new boolean[LEAF_ADDR.length], ns = new boolean[LEAF_ADDR.length];
	private String tempSensing1 = "";
    private int nSample = 0;
	public void runs() {
		senseX = "#";
		senseY = "#";
		senseZ = "#";
		try {
			for(int i=0;i<LEAF_ADDR.length;i++) {
				agr[i] = false;
				ns[i] = false;
			}
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, SELF_ADDR, SELF_ADDR, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);
			send_receive(fio);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void send_receive(final FrameIO fio) throws Exception {
		Thread thread = new Thread() {
			public void run() {
				Frame frame = new Frame();
				while (true) {
					try {
						fio.receive(frame);
						byte[] dg = frame.getPayload();
						String str = new String(dg, 0, dg.length);
						if (str.equalsIgnoreCase("STOP")) {
							stop = true;
							send("STOPPED",SELF_ADDR, BASE_ADDR,fio);
						} 
						
						else if(str.charAt(0)=='t'){
							String time = str.substring(1);
							long curTime = Long.parseLong(time)+7;
							Time.setCurrentTimeMillis(curTime);
							long t1 = Time.currentTimeMillis()+200;
							String msg = "Time " + Integer.toHexString(SELF_ADDR) + " " + t1;
							send(msg,SELF_ADDR, BASE_ADDR,fio);
							for (int i=0;i<LEAF_ADDR.length; i++) {
								String message = "t" + t1;
								send(message, SELF_ADDR, LEAF_ADDR[i], fio);
							}
							
						}
						else if(str.charAt(0)=='T'||str.charAt(str.length() - 1) == 'E') {
							send(str,SELF_ADDR, BASE_ADDR,fio);
						}
						
						else if(str.equalsIgnoreCase("AGGR1")) {
							stop = false;
							timeAggr = 500;
							timeStart = System.currentTimeMillis();
							for(int i=0;i<LEAF_ADDR.length;i++) {
								send("AGGR",SELF_ADDR,LEAF_ADDR[i],fio);
								Thread.sleep(5);
							}
						}
						else if(str.equalsIgnoreCase("AGGR2")) {
							stop = false;
							timeAggr = 1000;
							timeStart = System.currentTimeMillis();
							for(int i=0;i<LEAF_ADDR.length;i++) {
								send("AGGR",SELF_ADDR,LEAF_ADDR[i],fio);
								Thread.sleep(5);
							}
						}
						else if(str.equalsIgnoreCase("AGGR3")) {
							stop = false;
							timeAggr = 2000;
							timeStart = System.currentTimeMillis();
							for(int i=0;i<LEAF_ADDR.length;i++) {
								send("AGGR",SELF_ADDR,LEAF_ADDR[i],fio);
								Thread.sleep(5);
							}
						}
						else if(str.charAt(1)=='g'&&stop==false) {
							String[] temp = splitPagar(str);
							for(int i=0;i<LEAF_ADDR.length;i++) {
								if(LEAF_ADDR[i]==frame.getSrcAddr()) {
									senseAgrX[i] = Integer.parseInt(temp[1]);
									senseAgrY[i] = Integer.parseInt(temp[2]);
									senseAgrZ[i] = Integer.parseInt(temp[3]);
									agr[i] = true;
								}
							}
							
							boolean complete = true;
							for(int i=0;i<agr.length;i++) {
								if(agr[i]==false) {
									complete = false;
								}
							}
							if(complete) {
								long t1 = System.currentTimeMillis();
								if(t1-timeStart>=timeAggr) {
									double[][] x = new double[nSample][senseAgrX.length+1];
									double[][] y = new double[nSample][senseAgrY.length+1];
									double[][] z = new double[nSample][senseAgrZ.length+1];
									for(int i=0;i<nSample;i++) {
										double[] tempX1 = splitNilai(tempX.poll());
										double[] tempY1 = splitNilai(tempY.poll());
										double[] tempZ1 = splitNilai(tempZ.poll());
										for(int j=0;j<tempX1.length;j++) {
											x[i][j]=tempX1[j];
											y[i][j]=tempY1[j];
											z[i][j]=tempZ1[j];
										}
									}
									pcX = new PCA(x);
									pcY = new PCA(y);
									pcZ = new PCA(z);
									double[] resX = pcX.getPC();
									double[] resY = pcY.getPC();
									double[] resZ = pcZ.getPC();
									for(int i=0;i<resX.length;i++) {
										String msg = "AGR "+SELF_ADDR+" "+timeStart+" "+resX[i]+" "+resY[i]+" "+resZ[i];
										send(msg,SELF_ADDR,BASE_ADDR,fio);
									}
									send("DONE",SELF_ADDR,BASE_ADDR,fio);
									nSample = 0;
								}
								else {
									nSample+=1;
									int[] sense1 = sense();
									for(int i=0;i<senseAgrX.length;i++) {
										senseX += senseAgrX[i]+"#";
										senseY += senseAgrY[i]+"#";
										senseZ += senseAgrZ[i]+"#";
									}
									senseX += sense1[0]+"#";
									senseY += sense1[1]+"#";
									senseZ += sense1[2]+"#";
									tempX.add(senseX);
									tempY.add(senseY);
									tempZ.add(senseZ);
									for(int i=0;i<LEAF_ADDR.length;i++) {
										agr[i]=false;
									}
									senseX = "#";
									senseY = "#";
									senseZ = "#";
									for(int i=0;i<LEAF_ADDR.length;i++) {
										send("AGGR",SELF_ADDR,LEAF_ADDR[i],fio);
										Thread.sleep(5);
									}
								}
							}
						}
						else if (str.equalsIgnoreCase("DETECT")) {
							stop = false;
							int[] res = sense();
							for(int i=0;i<LEAF_ADDR.length;i++) {
								send("DETECT",SELF_ADDR, LEAF_ADDR[i], fio);
								Thread.sleep(5);
							}
							tempSensing1 = "SENSE "+ SELF_ADDR+" "+Time.currentTimeMillis()+" "+
									res[0]+" "+res[1]+" "+res[2];
						}
						else if(str.charAt(0) == 'S'&&stop==false) {
							for(int i=0;i<LEAF_ADDR.length;i++) {
								if(LEAF_ADDR[i]==frame.getSrcAddr()) {
									ns[i]=true;
									tempSensing[i] = str;
								}
							}
							boolean complete = true;
							for(int i=0;i<LEAF_ADDR.length;i++) {
								if(ns[i]==false) {
									complete = false;
								}
							}
							if(complete) {
								send(tempSensing1,SELF_ADDR,BASE_ADDR,fio);
								Thread.sleep(5);
								for(int i=0;i<LEAF_ADDR.length;i++) {
									ns[i]=false;
									send(tempSensing[i],SELF_ADDR,BASE_ADDR,fio);
									Thread.sleep(5);
								}
								
								for(int i=0;i<LEAF_ADDR.length;i++) {
									 send("DETECT",SELF_ADDR,LEAF_ADDR[i],fio);
									 Thread.sleep(5);
								 }
								int[] res = sense();
								tempSensing1 = "SENSE "+ SELF_ADDR+" "+Time.currentTimeMillis()+" "+
										res[0]+" "+res[1]+" "+res[2];
								
							}
						}
						else if (str.equalsIgnoreCase("ON")) {
							String msg = "Node " + Integer.toHexString(SELF_ADDR) + " ONLINE";
							send(msg, SELF_ADDR, BASE_ADDR, fio);
							for(int i=0;i<LEAF_ADDR.length;i++) {
								send(str,SELF_ADDR,LEAF_ADDR[i],fio);
							}
							
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}
	
	public int[] sense() throws Exception{
		AS.run();
		int[] res = AS.getTemp();
		return res;
	}
	public double[] splitNilai(String kata) {
		double[] res = new double[LEAF_ADDR.length+1];
		int k=1;
		for(int i=0;i<res.length;i++) {
			String temp ="";
			for(int j=k;j<kata.length()-1;j++) {
				if(kata.charAt(j)!='#') {
					temp+=kata.charAt(j);
				}
				else {
					k=j+1;
					break;
				}
			}
			res[i] = Double.parseDouble(temp);
		}
		return res;
	}
	public String[] splitPagar(String kata) {
		String[] res = new String[5];
		int k=0;
		for(int i=0;i<res.length;i++) {
			String temp ="";
			for(int j=k;j<kata.length();j++) {
				if(kata.charAt(j)!='#') {
					temp+=kata.charAt(j);
				}
				else {
					k=j+1;
					break;
				}
			}
			res[i]=temp;
		}
		return res;
	}
	
	public void send(String message, int source, int destination, final FrameIO fio) {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.ACK_REQUEST
				| Frame.SRC_ADDR_16;
		final Frame testFrame = new Frame(frameControl);
		testFrame.setDestPanId(COMMON_PANID);
		testFrame.setDestAddr(destination);
		testFrame.setSrcAddr(source);
		testFrame.setPayload(message.getBytes());
		try {
			fio.transmit(testFrame);
			Thread.sleep(50);
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) throws Exception {
		new ClusterHead().runs();
	}
}