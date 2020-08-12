import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

public class NodeSensor {
	private AccelerationSensor AS = new AccelerationSensor();
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] nodes = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	
	

	private static int CH_ADDR = nodes[1];
	private static int SELF_ADDR = nodes[2];
	
//	private static int CH_ADDR = nodes[3];
//	private static int SELF_ADDR = nodes[4];

//	private static int CH_ADDR = nodes[3];
//	private static int SELF_ADDR = nodes[5];

	public void runs() {
		try {
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
						
						if(str.charAt(0)=='t'){
							String time = str.substring(1);
							long curTime = Long.parseLong(time)+7;
							Time.setCurrentTimeMillis(curTime);
							String msg = "Time " + Integer.toHexString(SELF_ADDR) + " " + time;
							send(msg,SELF_ADDR, CH_ADDR,fio);
						}
						else if (str.equalsIgnoreCase("ON")) {
							String msg = "Node " + Integer.toHexString(SELF_ADDR) + " ONLINE";
							send(msg, SELF_ADDR, CH_ADDR, fio);
							
						} 
						else if(str.equalsIgnoreCase("AGGR")) {
							int[] res = sense();
							String msg = "agr";
							
							msg = msg+"#"+res[0]+"#"+res[1]+"#"+res[2]+"#"+Integer.toHexString(SELF_ADDR);
							send(msg, SELF_ADDR, CH_ADDR,fio);
						}
						else if (str.equalsIgnoreCase("DETECT")) {
							int[] res = sense();
							
							String message = "SENSE "+ SELF_ADDR+" "+Time.currentTimeMillis()+" "+
									res[0]+" "+res[1]+" "+res[2];
							send(message, SELF_ADDR, CH_ADDR, fio);
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
		new NodeSensor().runs();
	}
	
	
}
