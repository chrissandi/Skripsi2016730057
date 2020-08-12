package application;

import java.net.URL;

import models.Data;

import java.util.ResourceBundle;
import java.util.Date;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;


import com.virtenio.commander.io.DataConnection;
import com.virtenio.commander.toolsets.preon32.Preon32Helper;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;




public class HandlerAppsController implements Initializable{
	
	private ObservableList<Data> data;
	
	@FXML
	private TableView resTable;
	@FXML
	private Label statusLabel, modeLabel;
	@FXML
	private RadioButton t1,t2,t3;
	
	private boolean stop = true;
	private BufferedWriter writer; 

	private Preon32Helper nodeHelper;
	private DataConnection conn;
	private BufferedInputStream in;
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		data = FXCollections.observableArrayList();
		TableColumn address = new TableColumn("Alamat Sensor");
		TableColumn x = new TableColumn("X");
		TableColumn y = new TableColumn("Y");
		TableColumn z = new TableColumn("Z");
		TableColumn time = new TableColumn("Waktu");
		address.setPrefWidth(110);
		x.setPrefWidth(125);
		y.setPrefWidth(125);
		z.setPrefWidth(125);
		time.setPrefWidth(175);
		resTable.getColumns().addAll(address,time,x,y,z);
		
		
		address.setCellValueFactory(new PropertyValueFactory<>("alamat"));
		time.setCellValueFactory(new PropertyValueFactory<>("time"));
		x.setCellValueFactory(new PropertyValueFactory<>("x"));
		y.setCellValueFactory(new PropertyValueFactory<>("y"));
		z.setCellValueFactory(new PropertyValueFactory<>("z"));
		resTable.setItems(data);
		
		try {
			context_set("context.set.2");
			time_synchronize();
			nodeHelper = new Preon32Helper("COM7", 115200);
			conn = nodeHelper.runModule("baseStation");
			in = new BufferedInputStream(conn.getInputStream());
			conn.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	@FXML
	public void onStopButtonClicked(ActionEvent event) {
		
		int pilihan = 0;
		if(stop==false) {
			try {
				conn.write(pilihan);
				Thread.sleep(8);
				conn.write(pilihan);
				Thread.sleep(8);
				conn.write(pilihan);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	@FXML  
	public void onSyncButtonClicked(ActionEvent event) {
		int pilihan = 1;
		if(stop==true) {
			try {
				conn.write(pilihan);
				Thread.sleep(1000);
				byte[] buffer = new byte[2048];
				String s = "";
				String hasil = "";
				while(in.available()>0){
					in.read(buffer);
					s = new String(buffer);
					conn.flush();
					String[] ss = s.split("#");
					for (String res : ss) {
						if (res.startsWith("Time")) {
							String[] fin = res.split(" ");
							long time = Long.parseLong(fin[2]);
							String temp = stringFormat(time);
							hasil+= fin[0] + " " + fin[1].toUpperCase() + " " + temp +"\n";
						}
					}
					System.out.println(hasil);
				}
				statusLabel.setText(hasil);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void onCekButtonClicked(ActionEvent event) {
		int pilihan = 2;
		if(stop==true) {
			try {
				conn.write(pilihan);
				Thread.sleep(2000);
				byte[] buffer = new byte[2048];
				String s = "";
				String hasil = "";
				while(in.available()>0){
					in.read(buffer);
					s = new String(buffer);
					conn.flush();
					String[] ss = s.split("#");
					for (String res : ss) {
						if (res.startsWith("Node")) {
							String[] temp = res.split(" ");
							String node = temp[0];
							String alamat = temp[1].toUpperCase();
							String on = temp[2];
							hasil+=node+" "+alamat+" "+on+"\n";
						}
					}
					System.out.println(hasil);
				}
				statusLabel.setText(hasil);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void onSenseButtonClicked(ActionEvent event){
		int pilihan = 3;
		if(stop==true) {
			stop = false;
			data.clear();
			this.modeLabel.setText("Sensing..");
			try {
				conn.write(pilihan);
				String fName = System.currentTimeMillis()+"";
				fName = "Sensing_" + fName + ".txt";
				writeToFile(fName,"Tester", in);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@FXML
	public void onAgrButtonClicked(ActionEvent event) {
		int pilihan =0;
		if(stop==true) {
			stop = false;
			data.clear();
			this.modeLabel.setText("Aggregating..");
			String fName = System.currentTimeMillis() + "";
			if(t1.isSelected()==true) {
				pilihan = 4;
				fName = "Aggregating_0.5 " + fName + ".txt";
			}
			else if(t2.isSelected()==true) {
				pilihan = 5;
				fName = "Aggregating_1 " + fName + ".txt";
			}
			else {
				pilihan =6;
				fName = "Aggregating_2 " + fName + ".txt";
			}
			try {
				conn.write(pilihan);
				writeToFile(fName,"Tester", in);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static DefaultLogger getConsoleLogger() {
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);

		return consoleLogger;
	}
	public void time_synchronize() throws Exception {
		DefaultLogger consoleLogger = getConsoleLogger();
		File buildFile = new File("D:\\Semester 8\\Skripsi 2\\Program Skripsi\\Sandbox\\build.xml");
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		antProject.addBuildListener(consoleLogger);

		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.ProjectHelper", helper);
			helper.parse(antProject, buildFile);
			String target = "cmd.time.synchronize";
			antProject.executeTarget(target);
			antProject.fireBuildFinished(null);
		} catch (BuildException e) {
			e.printStackTrace();
		}
	}
	public void writeToFile(String fName, String folName, BufferedInputStream in) throws Exception {
		Thread t = new Thread() {
			byte[] buffer = new byte[2048];
			String s;
			File newFolder = new File(folName);
			
			public void run() {
				if (!newFolder.exists())
					newFolder.mkdir();
				String path = folName + "/" + fName;
				try {
					FileWriter fw = new FileWriter(path);
					writer = new BufferedWriter(fw);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				while (stop==false) {		
					try {
						if (in.available() >0) {
							in.read(buffer);
							s = new String(buffer);
							if(s.charAt(0)=='S') {
								stop = true;
								break;
							}
							String[] subStr = s.split("#");
							for (String w : subStr) {
								if (w.startsWith("SENSE")||w.startsWith("AGR")) {
									String[] ss = w.split(" ");
									long val = Long.parseLong(ss[2]);
									String waktu = stringFormat(val);
									String alamat = Integer.toHexString(Integer.parseInt(ss[1])).toUpperCase();
									float x = Float.parseFloat(ss[3]);
									float y = Float.parseFloat(ss[4]);
									float z = Float.parseFloat(ss[5]);
									ss[1]= alamat;
									ss[2] = stringFormat(val);
									String newString = ss[0]+" "+ss[1].toUpperCase()+" "+ss[2]+" "+" "+ss[3]+" "+ss[4]+" "+ss[5];
									Data masukan = new Data(alamat,waktu,x,y,z);
									data.add(masukan);
									writer.write(newString, 0, newString.length());
									writer.newLine();
								}
							}
						}
					} catch (Exception e) {
					}
				}
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.start();
	}
	public String stringFormat(long val) {
		Date date = new Date(val); 
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String dateText = df.format(date);
		return dateText;
	}
	public void context_set(String target) throws Exception {
		DefaultLogger consoleLogger = getConsoleLogger();
		File buildFile = new File("D:\\Semester 8\\Skripsi 2\\Program Skripsi\\Sandbox\\buildUser.xml");
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		antProject.addBuildListener(consoleLogger);

		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.ProjectHelper", helper);
			helper.parse(antProject, buildFile);

			antProject.executeTarget(target);
			antProject.fireBuildFinished(null);
		} catch (BuildException e) {
			e.printStackTrace();
		}
	}
}
