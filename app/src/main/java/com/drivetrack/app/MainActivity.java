package com.drivetrack.app;
import android.Manifest;import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.graphics.*;import android.view.*;import android.widget.*;import org.json.*;import java.text.*;import java.util.*;
public class MainActivity extends Activity {
 final int bg=0xff0c131a,panel=0xff19242e,teal=0xff00ddb3;
 LinearLayout root,body;TextView gpsValue,distanceValue,driveTimeValue,currentLocationValue;
 Gauge gauge;boolean miles=false;int screen=0;
 Handler handler=new Handler();
 BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(screen==0)updateDashboard();}};
 int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
  miles=getPreferences(0).getBoolean("miles",false);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,new IntentFilter(TrackingService.UPDATE),Context.RECEIVER_NOT_EXPORTED);
  else registerReceiver(receiver,new IntentFilter(TrackingService.UPDATE));
  showDashboard();handler.postDelayed(new Runnable(){public void run(){if(screen==0)updateDashboard();handler.postDelayed(this,1200);}},1200);
 }
 @Override public void onDestroy(){unregisterReceiver(receiver);super.onDestroy();}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(10),dp(10),dp(10),dp(10));return t;}
 void init(String title){
  root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg);
  // Android 15+ draws apps edge-to-edge; keep the header and bottom tabs outside system bars.
  root.setOnApplyWindowInsetsListener((view,insets)->{
   int top,bottom;
   if(Build.VERSION.SDK_INT>=30){
    android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.statusBars()|android.view.WindowInsets.Type.navigationBars());
    top=bars.top;bottom=bars.bottom;
   }else{
    top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();
   }
   view.setPadding(0,top,0,bottom);
   return insets;
  });
  setContentView(root);root.requestApplyInsets();
  TextView h=text("◉ DriveTrack   "+title,21,teal);root.addView(h);
  ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(12),0,dp(12),0);scroll.addView(body);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout nav=new LinearLayout(this);
  for(String label:new String[]{"Drive","History","Settings"}){
   Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(teal);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
   nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));
   b.setOnClickListener(v->{if(label.equals("Drive"))showDashboard();else if(label.equals("History"))showHistory();else showSettings();});
  }root.addView(nav);
 }
 TextView card(String label,String value){
  LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(10),dp(6),dp(10),dp(6));c.setBackgroundColor(panel);
  c.addView(text(label,12,0xffa0b4c3));TextView val=text(value,23,0xffffffff);c.addView(val);
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(9);body.addView(c,p);return val;
 }
 String dist(double meters){return String.format(Locale.getDefault(),"%.2f %s",meters/(miles?1609.344:1000),miles?"mi":"km");}
 String speed(double ms){return String.format(Locale.getDefault(),"%.1f %s",ms*(miles?2.236936:3.6),miles?"mph":"km/h");}
 String duration(long ms){long m=Math.max(0,ms/60000);return String.format(Locale.getDefault(),"%02d:%02d",m/60,m%60);}
 String date(long ms){return new SimpleDateFormat("EEE, d MMM yyyy · HH:mm",Locale.getDefault()).format(new Date(ms));}
 void showDashboard(){
  screen=0;init("Dashboard");gpsValue=card("GPS STATUS","Ready");
  FrameLayout dial=new FrameLayout(this);
  gauge=new Gauge(this);dial.addView(gauge,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setPadding(dp(18),0,dp(18),dp(4));
  FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(64),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);dial.addView(controls,cp);
  if(!TrackingService.active){
   Button start=journeyButton("▶ Start",0xff068b60);
   LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(168),dp(52));controls.addView(start,bp);
   start.setOnClickListener(v->{
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
     requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},42);
    else startJourney();
   });
  }else{
   Button pause=journeyButton(TrackingService.paused?"▶ Resume":"Ⅱ Pause",0xffd1840a);
   Button stop=journeyButton("■ Stop",0xffd93749);
   LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,dp(52),1);p1.rightMargin=dp(10);
   controls.addView(pause,p1);controls.addView(stop,new LinearLayout.LayoutParams(0,dp(52),1));
   pause.setOnClickListener(v->{
    startService(new Intent(this,TrackingService.class).setAction(TrackingService.paused?"RESUME":"PAUSE"));
    handler.postDelayed(()->showDashboard(),180);
   });
   stop.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("End this journey?")
    .setMessage("Your journey will be saved to History.")
    .setNegativeButton("Keep driving",null)
    .setPositiveButton("Save & stop",(d,which)->{
     startService(new Intent(this,TrackingService.class).setAction("STOP"));
     handler.postDelayed(()->showDashboard(),300);
    }).show());
  }
  body.addView(dial,new LinearLayout.LayoutParams(-1,dp(320)));
  distanceValue=card("DISTANCE","0.00 km");driveTimeValue=card("JOURNEY TIME","00:00");
  currentLocationValue=card("CURRENT LOCATION","Waiting for GPS");
  updateDashboard();
 }
 Button journeyButton(String label,int color){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextColor(0xffffffff);b.setTextSize(16);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));return b;}
 void startJourney(){try{startForegroundService(new Intent(this,TrackingService.class));handler.postDelayed(()->showDashboard(),400);}
  catch(Exception ex){new AlertDialog.Builder(this).setMessage("Could not start GPS tracking: "+ex.getMessage()).setPositiveButton("OK",null).show();}}
 void updateDashboard(){
  if(screen!=0)return;
  if(gpsValue!=null)gpsValue.setText(TrackingService.active?(TrackingService.paused?"Journey paused":(TrackingService.accuracy>0?"Recording · GPS ±"+(int)TrackingService.accuracy+" m":"Connecting to GPS")):"Ready to start");
  if(distanceValue!=null)distanceValue.setText(dist(TrackingService.distance));
  if(driveTimeValue!=null)driveTimeValue.setText(TrackingService.active?duration(TrackingService.elapsedActiveMs()):"00:00");
  if(currentLocationValue!=null)currentLocationValue.setText(TrackingService.locationText);
  if(gauge!=null)gauge.invalidate();
 }
 @Override public void onRequestPermissionsResult(int req,String[] permissions,int[] results){super.onRequestPermissionsResult(req,permissions,results);
  if(req==42&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startJourney();
  else if(req==42)new AlertDialog.Builder(this).setMessage("Precise location is needed to track distance and speed.").setPositiveButton("OK",null).show();
 }
 void showHistory(){
  screen=1;init("Journey History");try(TripStore store=new TripStore(this);Cursor c=store.all()){
   int count=0;double total=0;while(c.moveToNext()){count++;total+=c.getDouble(c.getColumnIndexOrThrow("distance"));}
   card("TOTAL · "+count+" journeys",dist(total));c.moveToPosition(-1);
   if(count==0)card("NO JOURNEYS YET","Start your first journey from Drive.");
   while(c.moveToNext()){
    long id=c.getLong(c.getColumnIndexOrThrow("id")),start=c.getLong(c.getColumnIndexOrThrow("started")),end=c.getLong(c.getColumnIndexOrThrow("ended"));
    double d=c.getDouble(c.getColumnIndexOrThrow("distance")),max=c.getDouble(c.getColumnIndexOrThrow("maxspeed"));
    Button item=new Button(this);item.setAllCaps(false);item.setText(date(start)+"\n"+dist(d)+"  ·  "+duration(end-start)+"\nTop speed: "+speed(max));
    item.setTextColor(0xffffffff);item.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
    body.addView(item,new LinearLayout.LayoutParams(-1,dp(110)));item.setOnClickListener(v->showDetail(id));
   }
  }
 }
 void showDetail(long id){
  screen=2;init("Journey Details");
  try(TripStore store=new TripStore(this);Cursor c=store.one(id)){
   if(!c.moveToFirst())return;
   long start=c.getLong(c.getColumnIndexOrThrow("started")),end=c.getLong(c.getColumnIndexOrThrow("ended")),moving=c.getLong(c.getColumnIndexOrThrow("movingms"));
   double d=c.getDouble(c.getColumnIndexOrThrow("distance")),max=c.getDouble(c.getColumnIndexOrThrow("maxspeed"));
   card("DATE",date(start));card("DISTANCE",dist(d));card("TOP SPEED",speed(max));
   card("AVERAGE SPEED (INCLUDES STOPS)",speed(d/Math.max(1,(end-start-c.getLong(c.getColumnIndexOrThrow("pausedms")))/1000.0)));
   card("MOVING AVERAGE (ESTIMATE)",speed(d/Math.max(1,moving/1000.0)));
   card("DURATION",duration(end-start-c.getLong(c.getColumnIndexOrThrow("pausedms"))));
   card("STATIONARY TIME (ESTIMATE)",duration(Math.max(0,end-start-c.getLong(c.getColumnIndexOrThrow("pausedms"))-moving)));
   JSONArray pts=new JSONArray(c.getString(c.getColumnIndexOrThrow("points")));
   body.addView(new RouteView(this,pts),new LinearLayout.LayoutParams(-1,dp(310)));
   card("ROUTE","GPS trace · "+pts.length()+" points (no street tiles)");
  }catch(Exception e){card("Details unavailable",String.valueOf(e.getMessage()));}
 }
 void showSettings(){
  screen=3;init("Settings");
  Button units=new Button(this);units.setAllCaps(false);units.setText("Units: "+(miles?"mph and miles":"km/h and kilometres"));
  body.addView(units);units.setOnClickListener(v->{miles=!miles;getPreferences(0).edit().putBoolean("miles",miles).apply();showSettings();});
  card("PRIVACY","Journey records and GPS coordinates stay on your phone.");
  card("TRACKING","Uses a foreground GPS service and persistent Android notification.");
  Button delete=new Button(this);delete.setText("Delete all saved journeys");body.addView(delete);
  delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete all saved journeys?")
   .setMessage("This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{new TripStore(this).deleteAll();showHistory();}).show());
 }
 class Gauge extends View{
  Paint p=new Paint(3);Gauge(Context c){super(c);}
  protected void onDraw(Canvas c){super.onDraw(c);
   float w=getWidth(),h=getHeight(),cx=w/2,cy=h*.41f,r=Math.min(w*.40f,h*.43f);
   float display=(float)(TrackingService.currentSpeed*(miles?2.236936:3.6)),max=miles?120:200;
   p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(dp(10));p.setColor(panel);
   c.drawArc(cx-r,cy-r,cx+r,cy+r,140,260,false,p);
   p.setColor(teal);c.drawArc(cx-r,cy-r,cx+r,cy+r,140,260*Math.min(1,display/max),false,p);
   for(int i=0;i<=10;i++){double a=Math.toRadians(140+260*i/10.0);
    float x1=cx+(r-dp(24))*(float)Math.cos(a),y1=cy+(r-dp(24))*(float)Math.sin(a);
    float x2=cx+(r-dp(13))*(float)Math.cos(a),y2=cy+(r-dp(13))*(float)Math.sin(a);
    p.setStrokeWidth(dp(2));p.setColor(0xff8ca7b6);c.drawLine(x1,y1,x2,y2,p);
   }
   double a=Math.toRadians(140+260*Math.min(1,Math.max(0,display/max)));
   p.setStrokeWidth(dp(4));p.setColor(0xffffffff);c.drawLine(cx,cy,cx+(r-dp(32))*(float)Math.cos(a),cy+(r-dp(32))*(float)Math.sin(a),p);
   p.setStyle(Paint.Style.FILL);p.setColor(teal);c.drawCircle(cx,cy,dp(7),p);
   p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(52));p.setColor(0xffffffff);c.drawText(""+Math.round(display),cx,cy+dp(52),p);
   p.setTextSize(dp(16));p.setColor(0xffa0b4c3);c.drawText(miles?"mph":"km/h",cx,cy+dp(73),p);
   p.setTextSize(dp(12));c.drawText("CURRENT SPEED",cx,cy-dp(42),p);
  }
 }
 class RouteView extends View{
  JSONArray pts;Paint p=new Paint(3);RouteView(Context c,JSONArray a){super(c);pts=a;setBackgroundColor(panel);}
  protected void onDraw(Canvas c){super.onDraw(c);
   if(pts.length()<2){p.setColor(0xffffffff);p.setTextSize(dp(14));c.drawText("Not enough GPS points to draw route",dp(12),dp(40),p);return;}
   try{
    double minLa=90,maxLa=-90,minLo=180,maxLo=-180;
    for(int i=0;i<pts.length();i++){JSONArray a=pts.getJSONArray(i);double la=a.getDouble(0),lo=a.getDouble(1);minLa=Math.min(minLa,la);maxLa=Math.max(maxLa,la);minLo=Math.min(minLo,lo);maxLo=Math.max(maxLo,lo);}
    double latSpan=Math.max(.0001,maxLa-minLa),lonSpan=Math.max(.0001,maxLo-minLo);
    double scale=Math.min((getWidth()-dp(40))/lonSpan,(getHeight()-dp(40))/latSpan);
    Path path=new Path();for(int i=0;i<pts.length();i++){JSONArray a=pts.getJSONArray(i);
     float x=(float)(dp(20)+(a.getDouble(1)-minLo)*scale),y=(float)(getHeight()-dp(20)-(a.getDouble(0)-minLa)*scale);
     if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
    }
    p.setColor(0xff28a4ff);p.setStrokeWidth(dp(4));p.setStyle(Paint.Style.STROKE);c.drawPath(path,p);
    p.setStyle(Paint.Style.FILL);p.setColor(teal);JSONArray a=pts.getJSONArray(0);
    c.drawCircle((float)(dp(20)+(a.getDouble(1)-minLo)*scale),(float)(getHeight()-dp(20)-(a.getDouble(0)-minLa)*scale),dp(7),p);
    p.setColor(0xffff5967);a=pts.getJSONArray(pts.length()-1);
    c.drawCircle((float)(dp(20)+(a.getDouble(1)-minLo)*scale),(float)(getHeight()-dp(20)-(a.getDouble(0)-minLa)*scale),dp(7),p);
   }catch(Exception ignored){}
  }
 }
}
