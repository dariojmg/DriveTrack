package com.drivetrack.app;
import android.Manifest;import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.graphics.*;import android.view.*;import android.widget.*;import org.json.*;import java.text.*;import java.util.*;
public class MainActivity extends Activity {
 final int bg=0xff0c131a,panel=0xff19242e,teal=0xff00ddb3;
 LinearLayout root,body;TextView gpsValue,distanceValue,driveTimeValue,currentLocationValue;
 LinearLayout dashboardStats;
 Gauge gauge;boolean miles=false;int screen=0;
 static final int EXPORT_REQUEST=781,IMPORT_REQUEST=782;
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
  TextView h=text("◉ DriveTrack   "+title,18,teal);h.setPadding(dp(12),dp(4),dp(12),dp(4));root.addView(h);
  body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(10),0,dp(10),0);
  if(title.equals("Dashboard"))root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  else {ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
  LinearLayout nav=new LinearLayout(this);
  for(String label:new String[]{"Drive","History","Settings"}){
   Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(teal);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
   nav.addView(b,new LinearLayout.LayoutParams(0,dp(50),1));
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
  screen=0;init("Dashboard");
  // Compact, bounded layout: the dial absorbs remaining screen height rather than making the page scroll.
  LinearLayout gpsRow=new LinearLayout(this);gpsRow.setGravity(Gravity.CENTER_VERTICAL);
  gpsRow.setPadding(dp(12),0,dp(12),0);gpsRow.setBackgroundColor(panel);
  TextView gpsLabel=text("GPS",11,0xffa0b4c3);gpsRow.addView(gpsLabel);
  gpsValue=text("Ready to start",14,0xffffffff);
  gpsValue.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
  gpsRow.addView(gpsValue,new LinearLayout.LayoutParams(0,-1,1));
  body.addView(gpsRow,new LinearLayout.LayoutParams(-1,dp(44)));
  FrameLayout dial=new FrameLayout(this);dial.setClipChildren(false);
  gauge=new Gauge(this);dial.addView(gauge,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);
  controls.setPadding(dp(10),0,dp(10),0);
  FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(52),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
  dial.addView(controls,cp);
  if(!TrackingService.active){
   Button start=journeyButton("▶ Start",0xff078d62);
   controls.addView(start,new LinearLayout.LayoutParams(dp(160),dp(48)));
   start.setOnClickListener(v->{
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
     requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},42);
    else startJourney();
   });
  }else{
   Button pause=journeyButton(TrackingService.paused?"▶ Resume":"Ⅱ Pause",0xffc77c0a);
   Button stop=journeyButton("■ Stop",0xffd93749);
   LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,dp(48),1);p1.rightMargin=dp(8);
   controls.addView(pause,p1);controls.addView(stop,new LinearLayout.LayoutParams(0,dp(48),1));
   pause.setOnClickListener(v->{startService(new Intent(this,TrackingService.class).setAction(TrackingService.paused?"RESUME":"PAUSE"));handler.postDelayed(()->showDashboard(),180);});
   stop.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("End this journey?")
    .setMessage("Your journey will be saved to History.").setNegativeButton("Keep driving",null)
    .setPositiveButton("Save & stop",(d,which)->{startService(new Intent(this,TrackingService.class).setAction("STOP"));handler.postDelayed(()->showDashboard(),300);}).show());
  }
  LinearLayout.LayoutParams dialParams=new LinearLayout.LayoutParams(-1,0,1);
  dialParams.topMargin=dp(5);dialParams.bottomMargin=dp(6);
  body.addView(dial,dialParams);
  dashboardStats=new LinearLayout(this);dashboardStats.setOrientation(LinearLayout.HORIZONTAL);
  distanceValue=compactCard(dashboardStats,"DISTANCE","0.00 km",true);
  driveTimeValue=compactCard(dashboardStats,"JOURNEY TIME","00:00",false);
  body.addView(dashboardStats,new LinearLayout.LayoutParams(-1,dp(83)));
  LinearLayout where=new LinearLayout(this);where.setOrientation(LinearLayout.VERTICAL);
  where.setBackgroundColor(panel);where.setPadding(dp(12),dp(5),dp(12),dp(4));
  TextView placeLabel=text("CURRENT LOCATION",11,0xffa0b4c3);
  placeLabel.setPadding(0,0,0,0);where.addView(placeLabel);
  currentLocationValue=text("Waiting for GPS",16,0xffffffff);
  currentLocationValue.setSingleLine(true);currentLocationValue.setEllipsize(android.text.TextUtils.TruncateAt.END);
  currentLocationValue.setPadding(0,dp(3),0,0);where.addView(currentLocationValue);
  LinearLayout.LayoutParams whereParams=new LinearLayout.LayoutParams(-1,dp(65));
  whereParams.topMargin=dp(6);body.addView(where,whereParams);
  updateDashboard();
 }
 TextView compactCard(LinearLayout row,String label,String value,boolean first){
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setBackgroundColor(panel);
  card.setPadding(dp(9),dp(6),dp(5),dp(5));
  TextView name=text(label,11,0xffa0b4c3);name.setPadding(0,0,0,dp(2));card.addView(name);
  TextView number=text(value,23,0xffffffff);number.setSingleLine(true);number.setPadding(0,0,0,0);card.addView(number);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
  if(first)lp.rightMargin=dp(5);else lp.leftMargin=dp(5);
  row.addView(card,lp);return number;
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
 void importJourneys(){
  if(TrackingService.active){Toast.makeText(this,"Stop the current journey before restoring a backup.",Toast.LENGTH_LONG).show();return;}
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");
  try{startActivityForResult(pick,IMPORT_REQUEST);}
  catch(Exception e){new AlertDialog.Builder(this).setMessage("Cannot open the file picker: "+e.getMessage()).setPositiveButton("OK",null).show();}
 }
 void exportJourneys(){
  Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  save.addCategory(Intent.CATEGORY_OPENABLE);save.setType("application/json");
  save.putExtra(Intent.EXTRA_TITLE,"DriveTrack-journeys.json");
  try{startActivityForResult(save,EXPORT_REQUEST);}
  catch(Exception e){new AlertDialog.Builder(this).setMessage("Unable to open Android save dialog: "+e.getMessage()).setPositiveButton("OK",null).show();}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  if(request==IMPORT_REQUEST){
   try(TripStore store=new TripStore(this)){
    int restored=store.importDatabase(this,data.getData());
    new AlertDialog.Builder(this).setTitle("Backup restored")
     .setMessage(restored+" journeys imported. Existing duplicates were skipped.")
     .setPositiveButton("View history",(d,w)->showHistory()).show();
   }catch(Exception e){new AlertDialog.Builder(this).setTitle("Import failed")
      .setMessage("Your existing journeys are unchanged. "+e.getMessage())
      .setPositiveButton("OK",null).show();}
   return;
  }
  if(request!=EXPORT_REQUEST)return;
  try(TripStore store=new TripStore(this);java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){
   if(out==null)throw new java.io.IOException("File not writable");
   byte[] bytes=store.exportAll().toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
   out.write(bytes);out.flush();
   Toast.makeText(this,"Journeys exported. Upload the JSON file to ChatGPT.",Toast.LENGTH_LONG).show();
  }catch(Exception e){new AlertDialog.Builder(this).setMessage("Export failed: "+e.getMessage()).setPositiveButton("OK",null).show();}
 }
 void showHistory(){
  screen=1;init("Journey History");Button export=new Button(this);export.setAllCaps(false);export.setText("Export all journeys · JSON");body.addView(export);export.setOnClickListener(v->exportJourneys());try(TripStore store=new TripStore(this);Cursor c=store.all()){
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
   card("ROUTE MAP","Street map (internet needed) · "+pts.length()+" points");
   FrameLayout mapFrame=new FrameLayout(this);
   body.addView(mapFrame,new LinearLayout.LayoutParams(-1,dp(310)));
   showStreetMap(mapFrame,pts);
   Button offline=new Button(this);offline.setAllCaps(false);offline.setText("Show GPS trace without map");
   body.addView(offline);
   offline.setOnClickListener(v->{mapFrame.removeAllViews();mapFrame.addView(new RouteView(this,pts),new FrameLayout.LayoutParams(-1,-1));offline.setEnabled(false);});
  }catch(Exception e){card("Details unavailable",String.valueOf(e.getMessage()));}
 }
 void showSettings(){
  screen=3;init("Settings");
  Button units=new Button(this);units.setAllCaps(false);units.setText("Units: "+(miles?"mph and miles":"km/h and kilometres"));
  body.addView(units);units.setOnClickListener(v->{miles=!miles;getPreferences(0).edit().putBoolean("miles",miles).apply();showSettings();});
  Button export=new Button(this);export.setText("Export journeys to JSON");body.addView(export);export.setOnClickListener(v->exportJourneys());
  Button restore=new Button(this);restore.setText("Restore journeys from .db backup");restore.setAllCaps(false);body.addView(restore);restore.setOnClickListener(v->importJourneys());
  card("PRIVACY","Journeys stay on this phone. Export shares them only when you choose. Route maps request map tiles from OpenStreetMap.");
  card("TRACKING","Uses a foreground GPS service and persistent Android notification.");
  Button delete=new Button(this);delete.setText("Delete all saved journeys");body.addView(delete);
  delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete all saved journeys?")
   .setMessage("This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{new TripStore(this).deleteAll();showHistory();}).show());
 }
 void showStreetMap(FrameLayout frame,JSONArray pts){
  if(pts.length()<2){frame.addView(new RouteView(this,pts),new FrameLayout.LayoutParams(-1,-1));return;}
  android.webkit.WebView map=new android.webkit.WebView(this);
  map.setBackgroundColor(panel);map.getSettings().setJavaScriptEnabled(true);
  frame.addView(map,new FrameLayout.LayoutParams(-1,-1));
  String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
    +"<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
    +"<style>html,body,#map{margin:0;height:100%;width:100%;background:#19242e}</style></head>"
    +"<body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"
    +"const coords="+pts.toString()+";const map=L.map('map');"
    +"L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(map);"
    +"let line=L.polyline(coords,{color:'#149cf2',weight:5}).addTo(map);"
    +"L.circleMarker(coords[0],{color:'#00ddb3',fillColor:'#00ddb3',fillOpacity:1,radius:8}).addTo(map);"
    +"L.circleMarker(coords[coords.length-1],{color:'#ff536b',fillColor:'#ff536b',fillOpacity:1,radius:8}).addTo(map);"
    +"map.fitBounds(line.getBounds(),{padding:[20,20]});"
    +"</script></body></html>";
  map.loadDataWithBaseURL("https://appassets.androidplatform.net/",html,"text/html","UTF-8",null);
 }
 class Gauge extends View{
  final Paint p=new Paint(3);
  Gauge(Context ctx){super(ctx);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);
   float w=getWidth(),h=getHeight(),cx=w/2f,cy=h*.34f;
   float r=Math.min(w*.425f,h*.335f);
   float speed=(float)(TrackingService.currentSpeed*(miles?2.236936:3.6)),maximum=miles?120:160;
   float fraction=Math.min(1,Math.max(0,speed/maximum));
   p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
   p.setStrokeWidth(dp(12));p.setColor(0xff233a4b);
   c.drawArc(cx-r,cy-r,cx+r,cy+r,140,260,false,p);
   p.setColor(0x7700d9d0);p.setStrokeWidth(dp(18));p.setShadowLayer(dp(10),0,0,0x8800e7d6);
   c.drawArc(cx-r,cy-r,cx+r,cy+r,140,260*fraction,false,p);
   p.clearShadowLayer();
   p.setColor(0xff10dfca);p.setStrokeWidth(dp(9));
   c.drawArc(cx-r,cy-r,cx+r,cy+r,140,260*fraction,false,p);
   for(int i=0;i<=40;i++){
    double angle=Math.toRadians(140+260*i/40d);
    boolean major=i%5==0;
    float a=r-dp(major?23:17),b=r-dp(10);
    p.setStrokeWidth(dp(major?2:1));p.setColor(major?0xffc0e2ed:0xff537080);
    c.drawLine(cx+a*(float)Math.cos(angle),cy+a*(float)Math.sin(angle),
       cx+b*(float)Math.cos(angle),cy+b*(float)Math.sin(angle),p);
   }
   p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
   p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(11));p.setColor(0xffabc7d9);
   for(int i=0;i<=8;i++){
    double angle=Math.toRadians(140+260*i/8d);
    float rr=r-dp(42);
    c.drawText(String.valueOf(Math.round(maximum*i/8f)),cx+rr*(float)Math.cos(angle),
      cy+rr*(float)Math.sin(angle)+dp(4),p);
   }
   // Centered instrument speed takes precedence over decorative needle.
   p.setColor(0xff9dbac8);p.setTextSize(dp(11));c.drawText("CURRENT SPEED",cx,cy-dp(34),p);
   p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
   p.setTextSize(dp(66));c.drawText(String.valueOf(Math.round(speed)),cx,cy+dp(35),p);
   p.setTypeface(Typeface.DEFAULT);p.setColor(0xffb6ccd5);p.setTextSize(dp(15));
   c.drawText(miles?"mph":"km/h",cx,cy+dp(58),p);
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
