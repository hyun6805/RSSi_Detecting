package com.subDB;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.subDB02.R;

import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * WIFI Scanner
 * 
 * @author Seon
 * 
 */
public class MainActivity extends Activity implements  OnClickListener,SensorEventListener {


	//========================================= variables settings start
	private static final String TAG = "WIFIScanner";

	// MAG Sensor vairables
	private SensorManager sensorManager;
	float[] Mag = new float[3];

	//	DB variables
	String CurrentDate;
	SQLiteDatabase db;
	boolean databaseCreated = false;
	boolean tableCreated = false;
	Calendar calendar = Calendar.getInstance();
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.KOREA);
	ThreadSensor mthread;
	BufferedWriter log;
	
	boolean ThreadStop = false;
	boolean Startflag = false;
	int MagCollectTime = 1000; //ms
	int Stopcount = 0;
	int MagCount = 0;
	
	//	 WifiManager variables
	WifiManager wifimanager;
	String[] measure_SSID = new String[300];
	String[] measure_BSSID = new String[300];
	String[] measure_RSSI = new String[300];
	
	int WifiCount;
	int notnull;

	//	 UI variables
	TextView textStatus;
	Button btnScanStart;
	Button btnScanStop;
	EditText DBname;
	TextView count;
	TextView wifilist;
	 int ScanCount = 0;
	String text = "";
	String result = "";

//========================================= variables settings end

//========================================= wifi related code start
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			System.out.println("mReceiver");

			final String action = intent.getAction();
			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				getWIFIScanResult(); // get WIFISCanResult
				wifimanager.startScan(); // for refresh
				WifiCount += 1;
//				Toast.makeText(MainActivity.this, "n = " + num, Toast.LENGTH_SHORT).show();	
/*				if(num == 100){ 	// 100 sample insert and stop
					unregisterReceiver(mReceiver); 
					ThreadStop = true;
					printToast(num +" wifi data collected");
				}
*/			} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
			}

		}
	};
	
	public void getWIFIScanResult() {
		System.out.println("getWIFIScanResult");

		int notnull = 0;
		Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
	        @Override
	        public int compare(ScanResult lhs, ScanResult rhs) {
	            return (lhs.level > rhs.level ? -1 : (lhs.level==rhs.level ? 0 : 1));
		}
	};
		//		lhs.level > rhs.level DESC (descent:내림차순) by RSSI , if "<" change mark, you can use ASC(ascent) 
	     List<ScanResult> mScanResult = wifimanager.getScanResults();
	     Collections.sort(mScanResult, comparator);
	 
		textStatus.setText("Scan count is \t" + ScanCount + "\t find Wifi:	  " + mScanResult.size() + " second:   " + (MagCount * 2 + 2) + "\n");
		textStatus.append("=======================================\n");
		for (int i = 0; i < mScanResult.size(); i++) {
			final ScanResult result = mScanResult.get(i);
			if (result == null){
				continue;
			}
			if(TextUtils.isEmpty(result.SSID) == true){
				continue; 
			}									
			// ignore null value
			else{ 
				measure_SSID[notnull] = result.SSID.toString();
				measure_BSSID[notnull] = result.BSSID.toString();
				measure_RSSI[notnull] = Integer.toString(result.level);
				
				System.out.println(notnull + " " +measure_SSID[notnull] + " " + measure_BSSID[notnull]);
				notnull = notnull +1;
				}	
				// not-null value insert array
			}
			for(int i = 0; i< 10; i++){
			textStatus.append((i + 1) + ": " + measure_BSSID[i]+ "\t "+ measure_RSSI[i]+ "\t " + measure_SSID[i] +	"\n");
			}		//System.out.println("append");

			// UI settings
		textStatus.append("=======================================\n");
	}
	
	public void initWIFIScan() {
		// init WIFISCAN
		ScanCount = 0;
		text = "";
		final IntentFilter filter = new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mReceiver, filter);
		wifimanager.startScan();
		Log.d(TAG, "initWIFIScan()");
		//System.out.println("initWIFIScan");

	}
//========================================= wifi related code end

//========================================= sensor related code start
	class ThreadSensor extends Thread {
			public void run()	{
				while(!ThreadStop)	{
					mHandler.sendEmptyMessage(0);
					try	{Thread.sleep((long)MagCollectTime);
					} catch(InterruptedException e) {;}
				}System.out.println("Thread");
			}		

		}
	Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			System.out.println("mHandler");

			// DB data add part
			
			insertRecord(DBname.getText().toString(),df.format(System.currentTimeMillis()),
					Mag[0], Mag[1], Mag[2],
					measure_SSID[0],	measure_SSID[1],	measure_SSID[2],	measure_SSID[3],	measure_SSID[4],	
					measure_SSID[5],	measure_SSID[6],	measure_SSID[7],	measure_SSID[8],	measure_SSID[9],

/*					measure_SSID[10],	measure_SSID[11],	measure_SSID[12],	measure_SSID[13],	measure_SSID[14],
					measure_SSID[15],	measure_SSID[16],	measure_SSID[17],	measure_SSID[18],	measure_SSID[19],
					measure_SSID[20],	measure_SSID[21],	measure_SSID[22],	measure_SSID[23],	measure_SSID[24],
					measure_SSID[25],	measure_SSID[26],	measure_SSID[27],	measure_SSID[28],	measure_SSID[29],*/
					
					measure_BSSID[0],	measure_BSSID[1],	measure_BSSID[2],	measure_BSSID[3],	measure_BSSID[4],
					measure_BSSID[5],	measure_BSSID[6],	measure_BSSID[7],	measure_BSSID[8],	measure_BSSID[9],
/*					measure_BSSID[10],	measure_BSSID[11],	measure_BSSID[12],	measure_BSSID[13],	measure_BSSID[14],
					measure_BSSID[15],	measure_BSSID[16],	measure_BSSID[17],	measure_BSSID[18],	measure_BSSID[19],
					measure_BSSID[20],	measure_BSSID[21],	measure_BSSID[22],	measure_BSSID[23],	measure_BSSID[24],
					measure_BSSID[25],	measure_BSSID[26],	measure_BSSID[27],	measure_BSSID[28],	measure_BSSID[29],*/
					
					measure_RSSI[0],	measure_RSSI[1],	measure_RSSI[2],	measure_RSSI[3],	measure_RSSI[4],
					measure_RSSI[5],	measure_RSSI[6],	measure_RSSI[7],	measure_RSSI[8],	measure_RSSI[9],
/*					measure_RSSI[10],	measure_RSSI[11],	measure_RSSI[12],	measure_RSSI[13],	measure_RSSI[14],
					measure_RSSI[15],	measure_RSSI[16],	measure_RSSI[17],	measure_RSSI[18],	measure_RSSI[19],
					measure_RSSI[20],	measure_RSSI[21],	measure_RSSI[22],	measure_RSSI[23],	measure_RSSI[24],
					measure_RSSI[25],	measure_RSSI[26],	measure_RSSI[27],	measure_RSSI[28],	measure_RSSI[29],*/
								Integer.toString(MagCount), Integer.toString(WifiCount)
					
					);
					MagCount = MagCount + 1;
					if (MagCount == 30){					// number of samples
						unregisterReceiver(mReceiver); 
						ThreadStop = true;
						printToast(MagCount +" sensor data collected");

					}
			count.setText(String.valueOf(MagCount));

			return false;


		}
	});
		
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent e) {
		switch(e.sensor.getType()) {
			case Sensor.TYPE_MAGNETIC_FIELD: {
				Mag[0] = e.values[0];	Mag[1] = e.values[1];	Mag[2] = e.values[2];
//				System.out.println(Mag[0]);
				break;
			}
		}
	} // end switch
//========================================= sensor related code end
		
//========================================= SQLite related code start
		void createDatabase(String name) {
			System.out.println("creating database [" + name + "].");
			
			try {
				db = openOrCreateDatabase(name, MODE_PRIVATE,null);
				databaseCreated = true;
				System.out.println("database is created.");
				

				} catch(Exception ex) {
					ex.printStackTrace();
					System.out.println("database is not created.");
			}		
		}
		
		void createTable(String name) {
			System.out.println("creating table [" + name + "].");
			db.execSQL("create table if not exists " + name + "(" 
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "Time text,"
					+ "MagX float,"	+ "MagY float,"	+ "MagZ float,"
					
					+"measure_SSID0,"+	"measure_SSID1,"+	"measure_SSID2,"+	"measure_SSID3,"+	"measure_SSID4,"+	
					"measure_SSID5,"+	"measure_SSID6,"+	"measure_SSID7,"+	"measure_SSID8,"+	"measure_SSID9,"+	
/*					"measure_SSID10,"+	"measure_SSID11,"+	"measure_SSID12,"+	"measure_SSID13,"+	"measure_SSID14,"+
					"measure_SSID15,"+	"measure_SSID16,"+	"measure_SSID17,"+	"measure_SSID18,"+	"measure_SSID19,"+	
					"measure_SSID20,"+	"measure_SSID21,"+	"measure_SSID22,"+	"measure_SSID23,"+	"measure_SSID24,"+	
					"measure_SSID25,"+	"measure_SSID26,"+	"measure_SSID27,"+	"measure_SSID28,"+	"measure_SSID29,"+*/
					
					"measure_BSSID0,"+	"measure_BSSID1,"+	"measure_BSSID2,"+	"measure_BSSID3,"+	"measure_BSSID4,"+
					"measure_BSSID5,"+	"measure_BSSID6,"+	"measure_BSSID7,"+	"measure_BSSID8,"+	"measure_BSSID9,"+
/*					"measure_BSSID10,"+	"measure_BSSID11,"+	"measure_BSSID12,"+	"measure_BSSID13,"+	"measure_BSSID14,"+
					"measure_BSSID15,"+	"measure_BSSID16,"+	"measure_BSSID17,"+	"measure_BSSID18,"+	"measure_BSSID19,"+
					"measure_BSSID20,"+	"measure_BSSID21,"+	"measure_BSSID22,"+	"measure_BSSID23,"+	"measure_BSSID24,"+
					"measure_BSSID25,"+	"measure_BSSID26,"+	"measure_BSSID27,"+	"measure_BSSID28,"+	"measure_BSSID29,"+*/

					"measure_RSSI0,"+	"measure_RSSI1,"+	"measure_RSSI2,"+	"measure_RSSI3,"+	"measure_RSSI4,"+
					"measure_RSSI5,"+	"measure_RSSI6,"+	"measure_RSSI7,"+	"measure_RSSI8,"+	"measure_RSSI9,"+
/*					"measure_RSSI10,"+	"measure_RSSI11,"+	"measure_RSSI12,"+	"measure_RSSI13,"+	"measure_RSSI14,"+
					"measure_RSSI15,"+	"measure_RSSI16,"+	"measure_RSSI17,"+	"measure_RSSI18,"+	"measure_RSSI19,"+
					"measure_RSSI20,"+	"measure_RSSI21,"+	"measure_RSSI22,"+	"measure_RSSI23,"+	"measure_RSSI24,"+
					"measure_RSSI25,"+	"measure_RSSI26,"+	"measure_RSSI27,"+	"measure_RSSI28,"+	"measure_RSSI29,"*/
					
					"MagCount," +"WifiCount);"
					);
			
			tableCreated = true;
		}
		
		void insertRecord(String name, String T, float MagX,float MagY,float MagZ, 

				String meansure_SSID0,	String meansure_SSID1,	String meansure_SSID2,	String meansure_SSID3,	String meansure_SSID4,
				String meansure_SSID5,	String meansure_SSID6,	String meansure_SSID7,	String meansure_SSID8,	String meansure_SSID9,
/*				String meansure_SSID10,	String meansure_SSID11,	String meansure_SSID12,	String meansure_SSID13,	String meansure_SSID14,
				String meansure_SSID15,	String meansure_SSID16,	String meansure_SSID17,	String meansure_SSID18,	String meansure_SSID19,
				String meansure_SSID20,	String meansure_SSID21,	String meansure_SSID22,	String meansure_SSID23,	String meansure_SSID24,
				String meansure_SSID25,	String meansure_SSID26,	String meansure_SSID27,	String meansure_SSID28,	String meansure_SSID29,*/
				
				String meansure_BSSID0,	String meansure_BSSID1,	String meansure_BSSID2,	String meansure_BSSID3,	String meansure_BSSID4,
				String meansure_BSSID5,	String meansure_BSSID6,	String meansure_BSSID7,	String meansure_BSSID8,	String meansure_BSSID9,
/*				String meansure_BSSID10,	String meansure_BSSID11,	String meansure_BSSID12,	String meansure_BSSID13,	String meansure_BSSID14,
				String meansure_BSSID15,	String meansure_BSSID16,	String meansure_BSSID17,	String meansure_BSSID18,	String meansure_BSSID19,
				String meansure_BSSID20,	String meansure_BSSID21,	String meansure_BSSID22,	String meansure_BSSID23,	String meansure_BSSID24,
				String meansure_BSSID25,	String meansure_BSSID26,	String meansure_BSSID27,	String meansure_BSSID28,	String meansure_BSSID29,
				*/
				String meansure_RSSI0,	String meansure_RSSI1,	String meansure_RSSI2,	String meansure_RSSI3,	String meansure_RSSI4,
				String meansure_RSSI5,	String meansure_RSSI6,	String meansure_RSSI7,	String meansure_RSSI8,	String meansure_RSSI9,
/*				String meansure_RSSI10,	String meansure_RSSI11,	String meansure_RSSI12,	String meansure_RSSI13,	String meansure_RSSI14,
				String meansure_RSSI15,	String meansure_RSSI16,	String meansure_RSSI17,	String meansure_RSSI18,	String meansure_RSSI19,
				String meansure_RSSI20,	String meansure_RSSI21,	String meansure_RSSI22,	String meansure_RSSI23,	String meansure_RSSI24,
				String meansure_RSSI25,	String meansure_RSSI26,	String meansure_RSSI27,	String meansure_RSSI28,	String meansure_RSSI29,*/
										
				String MagCount, String WifiCount
				) {
			//System.out.println("inserting records into table " +name + ".");
			//db.execSQL("insert into " + name + "(X,Y,Z,T) values ("+X+","+Y+","+Z+","+T+");" );
			ContentValues Values = new ContentValues();
			Values.put("Time",T);
			Values.put("MagX",MagX);
			Values.put("MagY",MagY);
			Values.put("MagZ",MagZ);

			Values.put("measure_SSID0", measure_SSID[0]);	Values.put("measure_SSID1", measure_SSID[1]);	Values.put("measure_SSID2", measure_SSID[2]);	Values.put("measure_SSID3", measure_SSID[3]);	Values.put("measure_SSID4", measure_SSID[4]);	Values.put("measure_SSID5", measure_SSID[5]);	Values.put("measure_SSID6", measure_SSID[6]);	Values.put("measure_SSID7", measure_SSID[7]);	Values.put("measure_SSID8", measure_SSID[8]);	Values.put("measure_SSID9", measure_SSID[9]);
			/*Values.put("measure_SSID10", measure_SSID[10]);	Values.put("measure_SSID11", measure_SSID[11]);	Values.put("measure_SSID12", measure_SSID[12]);	Values.put("measure_SSID13", measure_SSID[13]);	Values.put("measure_SSID14", measure_SSID[14]);	Values.put("measure_SSID15", measure_SSID[15]);	Values.put("measure_SSID16", measure_SSID[16]);	Values.put("measure_SSID17", measure_SSID[17]);	Values.put("measure_SSID18", measure_SSID[18]);	Values.put("measure_SSID19", measure_SSID[19]);
			Values.put("measure_SSID20", measure_SSID[20]);	Values.put("measure_SSID21", measure_SSID[21]);	Values.put("measure_SSID22", measure_SSID[22]);	Values.put("measure_SSID23", measure_SSID[23]);	Values.put("measure_SSID24", measure_SSID[24]);	Values.put("measure_SSID25", measure_SSID[25]);	Values.put("measure_SSID26", measure_SSID[26]);	Values.put("measure_SSID27", measure_SSID[27]);	Values.put("measure_SSID28", measure_SSID[28]);	Values.put("measure_SSID29", measure_SSID[29]);
			*/
			Values.put("measure_BSSID0", measure_BSSID[0]);	Values.put("measure_BSSID1", measure_BSSID[1]);	Values.put("measure_BSSID2", measure_BSSID[2]);	Values.put("measure_BSSID3", measure_BSSID[3]);	Values.put("measure_BSSID4", measure_BSSID[4]);	Values.put("measure_BSSID5", measure_BSSID[5]);	Values.put("measure_BSSID6", measure_BSSID[6]);	Values.put("measure_BSSID7", measure_BSSID[7]);	Values.put("measure_BSSID8", measure_BSSID[8]);	Values.put("measure_BSSID9", measure_BSSID[9]);	
			/*Values.put("measure_BSSID10", measure_BSSID[10]);	Values.put("measure_BSSID11", measure_BSSID[11]);	Values.put("measure_BSSID12", measure_BSSID[12]);	Values.put("measure_BSSID13", measure_BSSID[13]);	Values.put("measure_BSSID14", measure_BSSID[14]);	Values.put("measure_BSSID15", measure_BSSID[15]);	Values.put("measure_BSSID16", measure_BSSID[16]);	Values.put("measure_BSSID17", measure_BSSID[17]);	Values.put("measure_BSSID18", measure_BSSID[18]);	Values.put("measure_BSSID19", measure_BSSID[19]);
			Values.put("measure_BSSID20", measure_BSSID[20]);	Values.put("measure_BSSID21", measure_BSSID[21]);	Values.put("measure_BSSID22", measure_BSSID[22]);	Values.put("measure_BSSID23", measure_BSSID[23]);	Values.put("measure_BSSID24", measure_BSSID[24]);	Values.put("measure_BSSID25", measure_BSSID[25]);	Values.put("measure_BSSID26", measure_BSSID[26]);	Values.put("measure_BSSID27", measure_BSSID[27]);	Values.put("measure_BSSID28", measure_BSSID[28]);	Values.put("measure_BSSID29", measure_BSSID[29]);
			*/
			Values.put("measure_RSSI0", measure_RSSI[0]);	Values.put("measure_RSSI1", measure_RSSI[1]);	Values.put("measure_RSSI2", measure_RSSI[2]);	Values.put("measure_RSSI3", measure_RSSI[3]);	Values.put("measure_RSSI4", measure_RSSI[4]);	Values.put("measure_RSSI5", measure_RSSI[5]);	Values.put("measure_RSSI6", measure_RSSI[6]);	Values.put("measure_RSSI7", measure_RSSI[7]);	Values.put("measure_RSSI8", measure_RSSI[8]);	Values.put("measure_RSSI9", measure_RSSI[9]);
			/*Values.put("measure_RSSI20", measure_RSSI[20]);	Values.put("measure_RSSI21", measure_RSSI[21]);	Values.put("measure_RSSI22", measure_RSSI[22]);	Values.put("measure_RSSI23", measure_RSSI[23]);	Values.put("measure_RSSI24", measure_RSSI[24]);	Values.put("measure_RSSI25", measure_RSSI[25]);	Values.put("measure_RSSI26", measure_RSSI[26]);	Values.put("measure_RSSI27", measure_RSSI[27]);	Values.put("measure_RSSI28", measure_RSSI[28]);	Values.put("measure_RSSI29", measure_RSSI[29]);
			Values.put("measure_RSSI10", measure_RSSI[10]);	Values.put("measure_RSSI11", measure_RSSI[11]);	Values.put("measure_RSSI12", measure_RSSI[12]);	Values.put("measure_RSSI13", measure_RSSI[13]);	Values.put("measure_RSSI14", measure_RSSI[14]);	Values.put("measure_RSSI15", measure_RSSI[15]);	Values.put("measure_RSSI16", measure_RSSI[16]);	Values.put("measure_RSSI17", measure_RSSI[17]);	Values.put("measure_RSSI18", measure_RSSI[18]);	Values.put("measure_RSSI19", measure_RSSI[19]);
*/
			
			Values.put("MAGcount", MagCount);
			Values.put("WifiCount", WifiCount);

			
			db.insert(name, null, Values);	System.out.println("insertRecord");
		}			

//========================================= SQLite related code end	
	
//========================================= UI related code start	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

		// Setup UI
		textStatus = (TextView) findViewById(R.id.Status);
		btnScanStart = (Button) findViewById(R.id.ScanStart);
		btnScanStop = (Button) findViewById(R.id.ScanStop);
		DBname = (EditText) findViewById(R.id.DBname);
		count = (TextView) findViewById(R.id.SampleCount);
		
		// Setup OnClickListener
		btnScanStart.setOnClickListener(this);
		btnScanStop.setOnClickListener(this);

		// Setup WIFI
		wifimanager = (WifiManager) getSystemService(WIFI_SERVICE);
		Log.d(TAG, "Setup WIfiManager getSystemService");

		// if WIFIEnabled
		if (wifimanager.isWifiEnabled() == false)
			wifimanager.setWifiEnabled(true);
		
		mthread = new ThreadSensor();
		mthread.setDaemon(true);
	}
	
	public void printToast(String messageToast) {
		Toast.makeText(this, messageToast, Toast.LENGTH_LONG).show();
	}
//========================================= UI related code end	
	
//========================================= button click code start
	@Override


	public void onClick(View v) {					// Start button



		if (v.getId() == R.id.ScanStart) {
			Log.d(TAG, "OnClick() btnScanStart()");
			SystemClock.sleep(5000);
			MagCount = 0;
//			initWIFIScan(); // start WIFIScan
			if (Startflag == false) {
					if(DBname.getText().toString().equals("")) {
//					Toast.makeText(MainActivity.this, "put the Table name", Toast.LENGTH_LONG).show();
    				printToast("put the Table name");
					}
					else {
						initWIFIScan(); // start WIFIScan
						MagCount = 0;
						Startflag = true;
						printToast("Scan start");
						mthread.start();
						createDatabase(Environment.getExternalStorageDirectory().getAbsolutePath()+"/RSS/Data.db");
						createTable(DBname.getText().toString());
//								System.out.println("thread start");
					}
	    	}
    		else {
    			Toast.makeText(MainActivity.this, "Already Started", Toast.LENGTH_SHORT).show();
    		}
		}


		if (v.getId() == R.id.ScanStop){			//	stop button
			Log.d(TAG, "OnClick() btnScanStop()");
			SystemClock.sleep(5000);
			if (Startflag == true){
				Startflag = false;
				printToast("Stop");
				unregisterReceiver(mReceiver);    	//	stop WIFI scan
				ThreadStop = true;					
				}
			else {
    			Toast.makeText(MainActivity.this, "Already Stopped", Toast.LENGTH_SHORT).show();
    		}
		}
	}
//========================================= button click code end
	
//========================================= Activity menu related
	@Override
	protected void onResume() {

		super.onResume();
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		for(Sensor s : sensors) {
			sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST);

		}
		//requestLocationUpdates(Provider, minTime, minDistance, Listener);
		//0 means it updates ASAP it can.
	}
	
	@Override
	protected void onStop() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

}