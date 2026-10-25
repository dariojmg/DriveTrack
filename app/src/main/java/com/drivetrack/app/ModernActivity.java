package com.drivetrack.app;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import java.util.*;
import java.text.*;
import org.json.*;

public class ModernActivity extends MainActivity {
 final int ink=0xff07111c, surface=0xff122534, accent=0xff09ded0,muted=0xff9cb6c8;
 long selectedId=-1;int exportType=0;int monthOffset=0;String searchTerm="";boolean hud=false;
 static final int DETAIL_EXPORT=880;
 LinearLayout panelRoot; TextView digitalSpeed;boolean keepAwake=true; android.webkit.WebView liveMap; TextView mapPending;
 int px(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(px(radius));return g;}
 TextView label(String t,int sp,int col){TextView v=new TextView(this);v.setText(t);v.setTextSize(sp);v.setTextColor(col);v.setFontFeatureSettings("tnum");return v;}
 LinearLayout.LayoutParams margin(int height,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:height);p.bottomMargin=px(bottom);return p;}
 LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 void block(LinearLayout parent,View v,int height,int gap){parent.addView(v,margin(height,gap));}
 void headerSmall(LinearLayout parent,String heading,String sub){
  TextView title=label(heading,23,0xfff4f8fb);title.setTypeface(null,1);block(parent,title,-2,2);
  if(sub!=null){TextView desc=label(sub,12,muted);block(parent,desc,-2,10);}
 }
 Button button(String text,int color){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(null,1);b.setTextColor(-1);b.setMinHeight(0);b.setMinimumHeight(0);b.setBackgroundTintList(ColorStateList.valueOf(color));return b;}
 LinearLayout tile(String title,String value){
  LinearLayout l=vertical();l.setPadding(px(15),px(13),px(14),px(12));l.setBackground(round(surface,15));
  block(l,label(title,11,muted),-2,6);
  TextView v=label(value,23,-1);v.setTypeface(null,1);l.addView(v);return l;
 }
 void twoStats(LinearLayout holder,String a,String av,String b,String bv){
  LinearLayout row=new LinearLayout(this);
  LinearLayout left=tile(a,av),right=tile(b,bv);
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=px(5);row.addView(left,p);
  LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,-2,1);q.leftMargin=px(5);row.addView(right,q);
  block(holder,row,-2,10);
 }
 @Override void init(String title){
  root=vertical();root.setBackgroundColor(ink);
  root.setOnApplyWindowInsetsListener((v,in)->{
   int top,bottom;
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets z=in.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());top=z.top;bottom=z.bottom;}
   else{top=in.getSystemWindowInsetTop();bottom=in.getSystemWindowInsetBottom();}
   v.setPadding(0,top,0,bottom);return in;
  });
  setContentView(root);root.requestApplyInsets();
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(px(16),px(8),px(16),px(11));
  if(!title.equals("Dashboard")&&!title.equals("History")&&!title.equals("Settings")){
   TextView back=label("‹",32,accent);back.setGravity(Gravity.CENTER);back.setContentDescription("Back");
   header.addView(back,new LinearLayout.LayoutParams(px(48),px(48)));
   back.setOnClickListener(v->goBackFrom(title));
  }
  TextView brand=label("DT  DriveTrack",19,accent);brand.setTypeface(null,1);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));

  block(root,header,-2,0);
  body=vertical();body.setPadding(px(12),0,px(12),0);
  if(title.equals("Dashboard")||title.equals("HUD"))root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  else{ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
  LinearLayout nav=new LinearLayout(this);nav.setPadding(px(10),px(3),px(10),px(5));
  for(String entry:new String[]{"◉  Drive","▤  History","⚙  Settings"}){
   Button tab=button(entry,entry.contains(title.equals("Dashboard")||title.equals("HUD")?"Drive":title.equals("Settings")?"Settings":"History")?0xff19544d:surface);
   tab.setTextColor(entry.contains(title.equals("Dashboard")?"Drive":title.equals("Settings")?"Settings":"History")?accent:muted);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,px(49),1);lp.setMargins(px(2),0,px(2),0);nav.addView(tab,lp);
   tab.setOnClickListener(v->{hud=false;if(entry.contains("Drive"))showDashboard();else if(entry.contains("History"))showHistory();else showSettings();});
  }
  if(title.equals("Dashboard")||title.equals("History")||title.equals("Settings"))root.addView(nav);
 }
 void goBackFrom(String title){
  if(title.equals("Details")||title.equals("Insights"))showHistory();
  else if(title.equals("HUD")){getPreferences(0).edit().putBoolean("hud",false).apply();showDashboard();}
  else showDashboard();
 }
 @Override public void onBackPressed(){
  if(screen==2||screen==5)showHistory();
  else if(screen==4){getPreferences(0).edit().putBoolean("hud",false).apply();showDashboard();}
  else super.onBackPressed();
 }
 LinearLayout roundJourneyControl(int iconRes,String title,int color){
  GradientDrawable circle=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
   new int[]{lightenButton(color),color,darkenButton(color)});
  circle.setShape(GradientDrawable.OVAL);
  circle.setStroke(px(2),0xff4cf5e4);
  android.graphics.drawable.RippleDrawable ripple=new android.graphics.drawable.RippleDrawable(
    ColorStateList.valueOf(0x66ffffff),circle,null);
  LinearLayout control=vertical();
  control.setGravity(Gravity.CENTER);
  control.setBackground(ripple);
  control.setElevation(px(9));
  control.setClickable(true);
  control.setFocusable(true);
  control.setOnTouchListener((v,event)->{
   int action=event.getActionMasked();
   if(action==MotionEvent.ACTION_DOWN){
    v.animate().scaleX(.91f).scaleY(.91f).translationZ(-px(2)).setDuration(105).start();
   }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
    v.animate().scaleX(1f).scaleY(1f).translationZ(0).setDuration(140).start();
   }
   return false;
  });
  control.setContentDescription(title.equals("START")?"Start journey":title.equals("STOP")?"Stop and save journey":title+" journey");
  ImageView glyph=new ImageView(this);
  glyph.setImageResource(iconRes);
  glyph.setScaleType(ImageView.ScaleType.FIT_CENTER);
  control.addView(glyph,new LinearLayout.LayoutParams(px(30),px(39)));
  TextView caption=label(title,11,0xffffffff);caption.setTypeface(null,1);caption.setLetterSpacing(.05f);caption.setGravity(Gravity.CENTER);
  control.addView(caption,new LinearLayout.LayoutParams(-1,px(21)));
  return control;
 }
 int lightenButton(int color){return Color.rgb(Math.min(255,Color.red(color)+34),Math.min(255,Color.green(color)+34),Math.min(255,Color.blue(color)+34));}
 int darkenButton(int color){return Color.rgb((int)(Color.red(color)*.74),(int)(Color.green(color)*.74),(int)(Color.blue(color)*.74));}
 @Override void showDashboard(){
  screen=0;
  hud=getPreferences(0).getBoolean("hud",false);
  if(hud){showHud();return;}
  init("Dashboard");
  if(TrackingService.active){body.setAlpha(0f);body.animate().alpha(1f).setDuration(420).start();}
  if(!TrackingService.active){
   showReadyDashboard();
   return;
  }
  LinearLayout signal=new LinearLayout(this);signal.setPadding(px(14),px(8),px(14),px(8));signal.setGravity(Gravity.CENTER_VERTICAL);
  signal.setBackground(round(surface,16));
  signal.addView(label("◉   GPS STATUS",11,muted),new LinearLayout.LayoutParams(0,-2,1));
  gpsValue=label("Acquiring signal",12,accent);signal.addView(gpsValue);
  block(body,signal,px(40),4);
  FrameLayout dial=new FrameLayout(this);
  dial.setBackground(round(0xff081827,22));dial.setClipToOutline(true);
  gauge=new Gauge(this);dial.addView(gauge,new FrameLayout.LayoutParams(-1,-1));
  gauge.setAlpha(0f);gauge.post(()->gauge.animate().alpha(1f).setDuration(360).start());
  LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
  // Center round console-style controls beneath the dial, consistently in every journey state.
  FrameLayout.LayoutParams frame=new FrameLayout.LayoutParams(-1,px(100),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
  frame.bottomMargin=px(32);dial.addView(actions,frame);
  if(!TrackingService.active){
   LinearLayout start=roundJourneyControl(R.drawable.ic_drive_start,"START",0xff00a898);
   actions.addView(start,new LinearLayout.LayoutParams(px(94),px(94)));
   start.setOnClickListener(v->{if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},42);
    else startJourney();});
  }else{
   LinearLayout pause=roundJourneyControl(TrackingService.paused?R.drawable.ic_drive_resume:R.drawable.ic_drive_pause,TrackingService.paused?"RESUME":"PAUSE",0xff029d9e);
   LinearLayout stop=roundJourneyControl(R.drawable.ic_drive_stop,"STOP",0xff087c89);
   LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(px(94),px(94));left.rightMargin=px(14);
   actions.addView(pause,left);actions.addView(stop,new LinearLayout.LayoutParams(px(94),px(94)));
   pause.setOnClickListener(v->{startService(new Intent(this,TrackingService.class).setAction(TrackingService.paused?"RESUME":"PAUSE"));handler.postDelayed(()->showDashboard(),200);});
   stop.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Save this journey?")
    .setMessage("The trip will be saved to History and the dashboard will reset.")
    .setNegativeButton("Cancel",null).setPositiveButton("Save & stop",(d,w)->{startService(new Intent(this,TrackingService.class).setAction("STOP"));handler.postDelayed(()->showDashboard(),300);}).show());
  }
  boolean showMap=getPreferences(0).getBoolean("showLiveMap",true);
  // Let the dial take remaining space; map and stats retain guaranteed heights.
  LinearLayout.LayoutParams dialLayout=new LinearLayout.LayoutParams(-1,0,1);body.addView(dial,dialLayout);
  if(showMap){
   FrameLayout mini=new FrameLayout(this);mini.setBackground(round(0xff0a2434,16));mini.setClipToOutline(true);
   liveMap=new android.webkit.WebView(this);liveMap.setBackgroundColor(surface);
   liveMap.getSettings().setJavaScriptEnabled(true);
   liveMap.getSettings().setDomStorageEnabled(false);
   mini.addView(liveMap,new FrameLayout.LayoutParams(-1,-1));
   // A vertical shade keeps the speed numerals readable over moving streets.
   View shade=new View(this);
   GradientDrawable overlayShade=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
     new int[]{0xff081827,0xe5081827,0x7a081827,0x18081827});
   shade.setBackground(overlayShade);
   mini.addView(shade,new FrameLayout.LayoutParams(-1,-1));
   View overlay=new View(this);overlay.setClickable(true);overlay.setContentDescription("Open live map");
   mini.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
   TextView mapNote=label("● LIVE ROUTE",11,0xffc5f5f0);
   mapNote.setPadding(px(12),px(6),px(12),px(6));mapNote.setBackground(round(0xdd0b1b29,12));
   FrameLayout.LayoutParams notePosition=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.LEFT);
   notePosition.leftMargin=px(10);notePosition.bottomMargin=px(9);mini.addView(mapNote,notePosition);

   overlay.setOnClickListener(v->openLiveMap());
   // Slide the live map into the lower cockpit, underneath the gauge and controls.
   FrameLayout.LayoutParams mapLayout=new FrameLayout.LayoutParams(-1,-1);
   dial.addView(mini,0,mapLayout);
   mini.setAlpha(0f);
   mini.post(()->{
    mini.setTranslationY(px(130));
    mini.animate().translationY(0).alpha(1f).setDuration(580)
      .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
   });
   TextView pending=label("Searching for GPS satellites · Move outdoors for a stronger signal",12,0xffaec6d4);mapPending=pending;
   pending.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);pending.setPadding(px(18),0,px(18),px(145));
   mini.addView(pending,new FrameLayout.LayoutParams(-1,-1));
   if(TrackingService.lastFixTime>0&&TrackingService.active)pending.setVisibility(View.GONE);
   String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
    +"<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
    +"<style>html,body,#m{height:100%;margin:0;background:#0b2232}.leaflet-container{background:#07151e!important}.leaflet-tile-pane{filter:grayscale(1) invert(1) sepia(.3) hue-rotate(145deg) saturate(1.25) brightness(.44) contrast(1.2)}.leaflet-control-attribution{font-size:8px;background:#0b2232bb!important;color:#a3c3cb!important}</style></head><body><div id='m'></div>"
    +"<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"
    +"var m=L.map('m',{zoomControl:false,dragging:false,touchZoom:false,scrollWheelZoom:false});"
    +"L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:18,attribution:''}).addTo(m);"
    +"var trail=L.polyline([],{color:'#12d3bb',weight:4}).addTo(m);var dot=L.circleMarker([53.03,-7.3],{radius:7,color:'#ffffff',fillColor:'#12d3bb',fillOpacity:1});"
    +"window.moveTo=function(lat,lon){if(!Number.isFinite(lat)||!Number.isFinite(lon))return;"
    +"if(!m.hasLayer(dot))dot.addTo(m);dot.setLatLng([lat,lon]);m.setView([lat,lon],15,{animate:false});m.invalidateSize();};"
    +"window.showRoute=function(points){if(!Array.isArray(points))return;trail.setLatLngs(points);if(points.length){let p=points[points.length-1];window.moveTo(p[0],p[1]);}};"
    +"</script></body></html>";
   liveMap.loadDataWithBaseURL("https://appassets.androidplatform.net/",html,"text/html","UTF-8",null);
  }else {liveMap=null;mapPending=null;}
  dashboardStats=new LinearLayout(this);
  distanceValue=compactCard(dashboardStats,"DISTANCE","0.00 km",true);
  driveTimeValue=compactCard(dashboardStats,"DURATION","00:00",false);
  block(body,dashboardStats,px(68),7);
  LinearLayout place=tile("⌖   CURRENT LOCATION","Waiting for GPS");place.setPadding(px(15),px(8),px(14),px(7));
  currentLocationValue=(TextView)place.getChildAt(1);currentLocationValue.setTextSize(14);currentLocationValue.setSingleLine(true);
  currentLocationValue.setEllipsize(android.text.TextUtils.TruncateAt.END);
  block(body,place,px(65),6);
  updateDashboard();
 }
 void showReadyDashboard(){
  gpsValue=null;gauge=null;liveMap=null;mapPending=null;distanceValue=null;driveTimeValue=null;currentLocationValue=null;
  LinearLayout status=new LinearLayout(this);status.setGravity(Gravity.CENTER_VERTICAL);
  status.setPadding(px(16),px(13),px(16),px(13));status.setBackground(round(surface,17));
  status.addView(label("◉   GPS STATUS",12,muted),new LinearLayout.LayoutParams(0,-2,1));
  TextView state=label("Ready to record",12,accent);status.addView(state);
  block(body,status,px(53),8);
  LinearLayout center=vertical();center.setGravity(Gravity.CENTER);
  LinearLayout start=roundJourneyControl(R.drawable.ic_drive_start,"START",0xff029e91);
  center.addView(start,new LinearLayout.LayoutParams(px(112),px(112)));
  TextView heading=label("Ready for your journey?",24,Color.WHITE);heading.setTypeface(null,1);heading.setGravity(Gravity.CENTER);
  block(center,heading,-2,px(0));((LinearLayout.LayoutParams)heading.getLayoutParams()).topMargin=px(22);
  TextView subtitle=label("Tap Start to begin recording your drive",14,muted);
  subtitle.setGravity(Gravity.CENTER);block(center,subtitle,-2,8);
  body.addView(center,new LinearLayout.LayoutParams(-1,0,1));
  start.setOnClickListener(v->{start.animate().scaleX(.92f).scaleY(.92f).alpha(.4f).setDuration(140).withEndAction(()->{
   if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},42);
   else startJourney();
  }).start();});
  LinearLayout recent=vertical();recent.setPadding(px(18),px(14),px(18),px(12));recent.setBackground(round(surface,17));
  TextView recentTitle=label("LAST JOURNEY",11,muted);recentTitle.setLetterSpacing(.1f);block(recent,recentTitle,-2,8);
  String summary="No recorded journeys yet";
  try(TripStore db=new TripStore(this);Cursor cursor=db.all()){
   if(cursor.moveToFirst()){
    double d=cursor.getDouble(cursor.getColumnIndexOrThrow("distance"));
    long startMs=cursor.getLong(cursor.getColumnIndexOrThrow("started"));
    long endMs=cursor.getLong(cursor.getColumnIndexOrThrow("ended"));
    summary=dist(d)+"   ·   "+duration(Math.max(0,endMs-startMs));
   }
  }catch(Exception ignored){}
  TextView last=label(summary,19,Color.WHITE);last.setTypeface(null,1);block(recent,last,-2,0);
  block(body,recent,px(93),12);recent.setOnClickListener(v->showHistory());
 }
 @Override void updateDashboard(){
  updateScreenAwake();
  if(screen==0){
   if(gpsValue!=null)gpsValue.setText(TrackingService.active&&!TrackingService.gpsIssue.isEmpty()?TrackingService.gpsIssue:!TrackingService.active?"○ Waiting for GPS":TrackingService.paused?"Ⅱ Paused":TrackingService.lastFixTime>0&&System.currentTimeMillis()-TrackingService.lastFixTime>8000?"○ Signal lost":TrackingService.accuracy<=10&&TrackingService.lastFixTime>0?"● Strong · ±"+(int)TrackingService.accuracy+" m":TrackingService.accuracy<=25&&TrackingService.lastFixTime>0?"● Fair · ±"+(int)TrackingService.accuracy+" m":TrackingService.lastFixTime>0?"○ Poor · ±"+(int)TrackingService.accuracy+" m":"○ Acquiring GPS");
   if(distanceValue!=null)distanceValue.setText(dist(TrackingService.distance));
   if(driveTimeValue!=null)driveTimeValue.setText(TrackingService.active?duration(TrackingService.elapsedActiveMs()):"00:00");
   if(currentLocationValue!=null)currentLocationValue.setText(TrackingService.active?TrackingService.locationText:"Waiting for GPS");
   if(gauge!=null)gauge.invalidate();
   if(mapPending!=null){
    boolean fix=TrackingService.lastFixTime>0&&System.currentTimeMillis()-TrackingService.lastFixTime<12000;
    mapPending.setVisibility(fix?View.GONE:View.VISIBLE);
    if(!fix)mapPending.setText(!TrackingService.gpsIssue.isEmpty()?TrackingService.gpsIssue:TrackingService.lastFixTime>0?
      "GPS signal lost · Waiting for accurate coordinates":
      "Searching for GPS satellites · Move outdoors for a stronger signal");
   }
   if(liveMap!=null&&TrackingService.active&&TrackingService.lastFixTime>0)updateLiveMap();
  }else if(screen==4&&digitalSpeed!=null)digitalSpeed.setText(String.valueOf(Math.round(TrackingService.currentSpeed*(miles?2.236936:3.6))));
 }
 void updateLiveMap(){
  try{
   JSONArray pts=TrackingService.points;
   if(pts==null||pts.length()==0)return;
   JSONArray latest=pts.getJSONArray(pts.length()-1);
   double lat=latest.getDouble(0),lon=latest.getDouble(1);
   if(Math.abs(lat)>90||Math.abs(lon)>180)return;
   liveMap.evaluateJavascript("if(window.showRoute)window.showRoute("+pts.toString()+");",null);
  }catch(Exception ignored){}
 }
 void openLiveMap(){
  // The full-screen live view uses the device's mapping application until a journey is saved.
  if(TrackingService.active&&TrackingService.points!=null&&TrackingService.points.length()>0){
   try{
    JSONArray point=TrackingService.points.getJSONArray(TrackingService.points.length()-1);
    android.net.Uri uri=android.net.Uri.parse("geo:"+point.getDouble(0)+","+point.getDouble(1)+"?q="+point.getDouble(0)+","+point.getDouble(1));
    startActivity(new Intent(Intent.ACTION_VIEW,uri));
   }catch(Exception e){Toast.makeText(this,"Location unavailable",Toast.LENGTH_SHORT).show();}
  }else Toast.makeText(this,"Start a journey to view your live location",Toast.LENGTH_SHORT).show();
 }
 void updateScreenAwake(){
  if(keepAwake&&TrackingService.active&&!TrackingService.paused&&(screen==0||screen==4)&&hasWindowFocus())
   getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 }
 @Override protected void onResume(){super.onResume();keepAwake=getPreferences(0).getBoolean("keepAwake",true);updateScreenAwake();}
 @Override protected void onPause(){getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);super.onPause();}
 @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);updateScreenAwake();}
 void showHud(){
  screen=4;init("HUD");
  TextView note=label("MINIMAL DRIVING MODE · stationary interaction only",11,muted);note.setGravity(Gravity.CENTER);block(body,note,-2,12);
  digitalSpeed=label("0",110,-1);digitalSpeed.setGravity(Gravity.CENTER);digitalSpeed.setTypeface(null,1);
  body.addView(digitalSpeed,new LinearLayout.LayoutParams(-1,0,1));
  TextView units=label(miles?"mph":"km/h",20,accent);units.setGravity(Gravity.CENTER);block(body,units,-2,20);
  Button back=button("Return to dashboard",surface);block(body,back,px(50),12);
  back.setOnClickListener(v->{getPreferences(0).edit().putBoolean("hud",false).apply();hud=false;showDashboard();});
  updateDashboard();
  handler.postDelayed(new Runnable(){public void run(){if(screen==4){updateDashboard();handler.postDelayed(this,1000);}}},1000);
 }
 @Override void showHistory(){
  screen=1;init("History");headerSmall(body,"Your journeys","All your drives, organised by date");
  try(TripStore db=new TripStore(this);Cursor c=db.all()){
   int count=0;double sum=0;long totalMs=0;
   while(c.moveToNext()){count++;sum+=c.getDouble(c.getColumnIndexOrThrow("distance"));
    totalMs+=Math.max(0,c.getLong(c.getColumnIndexOrThrow("ended"))-c.getLong(c.getColumnIndexOrThrow("started"))-c.getLong(c.getColumnIndexOrThrow("pausedms")));
   }
   twoStats(body,"TOTAL DISTANCE",dist(sum),"JOURNEYS",String.valueOf(count));
   TextView drivingTime=label("◷   "+duration(totalMs)+" recorded drive time",13,muted);block(body,drivingTime,-2,12);
  }

  LinearLayout filters=new LinearLayout(this);
  Button earlier=button("‹",surface),later=button("›",surface);
  Calendar month=Calendar.getInstance();month.add(Calendar.MONTH,monthOffset);
  TextView period=label(new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(month.getTime()),15,-1);period.setGravity(Gravity.CENTER);
  filters.addView(earlier,new LinearLayout.LayoutParams(px(50),px(42)));
  filters.addView(period,new LinearLayout.LayoutParams(0,px(42),1));
  filters.addView(later,new LinearLayout.LayoutParams(px(50),px(42)));
  block(body,filters,px(46),8);
  earlier.setOnClickListener(v->{monthOffset--;showHistory();});
  later.setOnClickListener(v->{if(monthOffset<0){monthOffset++;showHistory();}});
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setTextColor(-1);search.setHintTextColor(muted);
  search.setHint("Search journey names");search.setBackground(round(surface,12));search.setPadding(px(14),0,px(14),0);
  search.setText(searchTerm);block(body,search,px(47),8);
  LinearLayout entries=vertical();block(body,entries,-2,8);
  Runnable load=()->{
   entries.removeAllViews();
   int n=0;
   try(TripStore db=new TripStore(this);Cursor c=db.all()){
    while(c.moveToNext()){
     long start=c.getLong(c.getColumnIndexOrThrow("started"));
     Calendar t=Calendar.getInstance();t.setTimeInMillis(start);
     if(t.get(Calendar.YEAR)!=month.get(Calendar.YEAR)||t.get(Calendar.MONTH)!=month.get(Calendar.MONTH))continue;
     long id=c.getLong(c.getColumnIndexOrThrow("id"));
     String title=c.getString(c.getColumnIndexOrThrow("title"));if(title==null||title.trim().isEmpty())title="Journey · "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(start));
     if(!title.toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT)))continue;
     n++;
     String dis=dist(c.getDouble(c.getColumnIndexOrThrow("distance")));
     double max=c.getDouble(c.getColumnIndexOrThrow("maxspeed"));
     long time=c.getLong(c.getColumnIndexOrThrow("ended"))-start-c.getLong(c.getColumnIndexOrThrow("pausedms"));
     LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(px(9),px(9),px(10),px(9));card.setBackground(round(surface,17));
     String geojson=c.getString(c.getColumnIndexOrThrow("points"));
     View thumbnail=new RouteThumbnail(this,geojson);
     LinearLayout.LayoutParams thumbLp=new LinearLayout.LayoutParams(px(78),px(82));thumbLp.rightMargin=px(12);
     card.addView(thumbnail,thumbLp);
     LinearLayout info=vertical();card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
     LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
     TextView tripName=label(title,17,-1);tripName.setTypeface(null,1);
     heading.addView(tripName,new LinearLayout.LayoutParams(0,-2,1));
     TextView menu=label("⋮",25,muted);menu.setGravity(Gravity.CENTER);
     menu.setContentDescription("Options for "+title);menu.setClickable(true);
     heading.addView(menu,new LinearLayout.LayoutParams(px(44),px(42)));
     block(info,heading,-2,2);
     block(info,label(date(start)+"   ·   "+duration(time),11,muted),-2,7);
     TextView figures=label(dis+"   ·   "+speed(max)+" max",13,accent);figures.setTypeface(null,1);block(info,figures,-2,0);
     block(entries,card,px(104),8);
     card.setOnClickListener(v->showDetail(id));
     menu.setOnClickListener(v->{
      PopupMenu popup=new PopupMenu(this,menu);
      popup.getMenu().add("View journey");
      popup.getMenu().add("Rename / add notes");
      popup.getMenu().add("Export GPX");
      popup.getMenu().add("Export CSV");
      popup.getMenu().add("Export JSON");
      popup.getMenu().add("Delete journey");
      final String editTitle=tripName.getText().toString(),editNotes=tripNotesFor(id);
      popup.setOnMenuItemClickListener(item->{
       String choice=item.getTitle().toString();
       if(choice.equals("View journey"))showDetail(id);
       else if(choice.equals("Rename / add notes"))editTrip(id,editTitle,editNotes);
       else if(choice.startsWith("Export "))exportTrip(id,choice.substring(7));
       else if(choice.equals("Delete journey"))confirmDeleteJourney(id);
       return true;
      });
      popup.show();
     });
    }
   }
   if(n==0)block(entries,label("No journeys for this month or search.",14,muted),-2,10);
  };
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int a,int c,int d){}
   public void onTextChanged(CharSequence s,int start,int before,int count){searchTerm=s.toString();load.run();}
   public void afterTextChanged(android.text.Editable e){}
  });
  load.run();
  Button insights=button("↗  View detailed statistics",surface);block(body,insights,px(45),8);insights.setOnClickListener(v->showInsights());
  Button export=button("Export journey history · JSON",surface);block(body,export,px(45),16);export.setOnClickListener(v->exportJourneys());
 }
 @Override void showDetail(long id){
  selectedId=id;screen=2;init("Details");
  try(TripStore db=new TripStore(this);Cursor c=db.one(id)){
   if(!c.moveToFirst())return;
   long started=c.getLong(c.getColumnIndexOrThrow("started")),ended=c.getLong(c.getColumnIndexOrThrow("ended"));
   long pause=c.getLong(c.getColumnIndexOrThrow("pausedms")),moving=c.getLong(c.getColumnIndexOrThrow("movingms"));
   double metres=c.getDouble(c.getColumnIndexOrThrow("distance")),max=c.getDouble(c.getColumnIndexOrThrow("maxspeed"));
   String tripTitle=c.getString(c.getColumnIndexOrThrow("title"));if(tripTitle==null||tripTitle.trim().isEmpty())tripTitle="Journey";
   String notes=c.getString(c.getColumnIndexOrThrow("notes"));if(notes==null)notes="";
   final String origTitle=tripTitle,origNotes=notes;
   headerSmall(body,tripTitle,date(started));
   if(!notes.trim().isEmpty()){
    LinearLayout noteCard=tile("NOTES",notes);
    ((TextView)noteCard.getChildAt(1)).setTextSize(14);
    block(body,noteCard,-2,10);
   }
   Button edit=button("✎  Rename / add notes",surface);block(body,edit,px(46),10);
   edit.setOnClickListener(v->editTrip(id,origTitle,origNotes));
   JSONArray coords=new JSONArray(c.getString(c.getColumnIndexOrThrow("points")));
   FrameLayout mapFrame=new FrameLayout(this);mapFrame.setBackground(round(surface,15));
   block(body,mapFrame,px(260),8);showStreetMap(mapFrame,coords);
   // A transparent preview shield allows the parent page to scroll normally. Tap to explore.
   View previewTap=new View(this);previewTap.setContentDescription("Expand journey map");
   previewTap.setClickable(true);
   mapFrame.addView(previewTap,new FrameLayout.LayoutParams(-1,-1));
   previewTap.setOnClickListener(v->openFullMap(id));
   Button expandMap=button("⛶  Expand interactive map",0xff17635b);
   block(body,expandMap,px(46),8);expandMap.setOnClickListener(v->openFullMap(id));
   Button mapFallback=button("Show offline GPS trace",surface);block(body,mapFallback,px(42),12);
   mapFallback.setOnClickListener(v->{mapFrame.removeAllViews();mapFrame.addView(new RouteView(this,coords),new FrameLayout.LayoutParams(-1,-1));});
   twoStats(body,"DISTANCE",dist(metres),"TOP SPEED",speed(max));
   long effective=Math.max(0,ended-started-pause);
   twoStats(body,"DURATION",duration(effective),"MOVING AVG",speed(metres/Math.max(1,moving/1000d)));
   LinearLayout technical=vertical();
   twoStats(technical,"OVERALL AVG",speed(metres/Math.max(1,effective/1000d)),"STOPPED",duration(Math.max(0,effective-moving)));
   twoStats(technical,"PAUSED",duration(pause),"GPS POINTS",String.valueOf(coords.length()));
   Button detailToggle=button("Technical details   ⌄",surface);block(body,detailToggle,px(44),8);
   technical.setVisibility(View.GONE);block(body,technical,-2,5);
   detailToggle.setOnClickListener(v->{boolean opening=technical.getVisibility()!=View.VISIBLE;
    technical.setVisibility(opening?View.VISIBLE:View.GONE);
    detailToggle.setText(opening?"Technical details   ⌃":"Technical details   ⌄");
   });
   String telemetry=c.getString(c.getColumnIndexOrThrow("telemetry"));
   try{JSONArray samples=new JSONArray(telemetry==null?"[]":telemetry);
    block(technical,label(samples.length()==0?"GPS diagnostics unavailable for this older journey":"GPS diagnostics: "+samples.length()+" speed samples",13,muted),-2,9);
   }catch(Exception ignored){}
   LinearLayout exports=new LinearLayout(this);
   for(String kind:new String[]{"GPX","CSV","JSON"}){
    Button b=button("Export "+kind,surface);exports.addView(b,new LinearLayout.LayoutParams(0,px(48),1));
    b.setOnClickListener(v->exportTrip(id,kind));
   }
   block(body,exports,px(49),14);
   Button remove=button("Delete this journey",0xff693039);
   remove.setTextColor(0xffffc2c6);
   block(body,remove,px(48),20);
   remove.setOnClickListener(v->confirmDeleteJourney(id));
  }catch(Exception e){block(body,label("Could not load journey: "+e.getMessage(),14,muted),-2,0);}
 }
 String tripNotesFor(long id){
  try(TripStore db=new TripStore(this);Cursor c=db.one(id)){
   if(c.moveToFirst()){String notes=c.getString(c.getColumnIndexOrThrow("notes"));return notes==null?"":notes;}
  }
  return "";
 }
 void confirmDeleteJourney(long id){
  new AlertDialog.Builder(this)
   .setTitle("Delete this journey?")
   .setMessage("This will permanently delete this journey and its GPS route. Other journeys will not be affected.")
   .setNegativeButton("Cancel",null)
   .setPositiveButton("Delete journey",(dialog,which)->{
    try(TripStore db=new TripStore(this)){db.deleteOne(id);}
    Toast.makeText(this,"Journey deleted",Toast.LENGTH_SHORT).show();
    showHistory();
   }).show();
 }
 void openFullMap(long id){
  Intent i=new Intent(this,JourneyMapActivity.class);
  i.putExtra("journey_id",id);
  startActivity(i);
 }
 void editTrip(long id,String title,String notes){
  LinearLayout form=vertical();form.setPadding(px(20),0,px(20),0);
  EditText name=new EditText(this);name.setSingleLine(true);name.setHint("e.g. Home to golf club");name.setText(title.equals("Journey")?"":title);form.addView(name);
  EditText memo=new EditText(this);memo.setHint("Optional notes");memo.setMinLines(2);memo.setText(notes);form.addView(memo);
  new AlertDialog.Builder(this).setTitle("Journey details").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
   try(TripStore store=new TripStore(this)){store.rename(id,name.getText().toString().trim(),memo.getText().toString().trim());}showDetail(id);
  }).show();
 }
 void showInsights(){
  screen=5;init("Insights");headerSmall(body,"Driving insights","Totals calculated from saved journeys");
  int count=0;double metres=0,longest=0,highest=0;long duration=0;
  TreeMap<String,Double> months=new TreeMap<>();
  try(TripStore db=new TripStore(this);Cursor c=db.all()){
   while(c.moveToNext()){
    count++;double d=c.getDouble(c.getColumnIndexOrThrow("distance"));metres+=d;longest=Math.max(longest,d);
    highest=Math.max(highest,c.getDouble(c.getColumnIndexOrThrow("maxspeed")));
    duration+=Math.max(0,c.getLong(c.getColumnIndexOrThrow("ended"))-c.getLong(c.getColumnIndexOrThrow("started"))-c.getLong(c.getColumnIndexOrThrow("pausedms")));
    String month=new SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date(c.getLong(c.getColumnIndexOrThrow("started"))));
    months.put(month,months.getOrDefault(month,0d)+d);
   }
  }
  twoStats(body,"JOURNEYS",""+count,"TOTAL DISTANCE",dist(metres));
  twoStats(body,"DRIVE TIME",duration(duration),"TOP SPEED",speed(highest));
  twoStats(body,"AVG JOURNEY",dist(count==0?0:metres/count),"LONGEST",dist(longest));
  headerSmall(body,"Distance by month","Your saved trip history");
  if(months.isEmpty())block(body,label("No journeys recorded yet",14,muted),-2,5);
  double maxMonth=1;for(double v:months.values())maxMonth=Math.max(maxMonth,v);
  for(Map.Entry<String,Double> item:months.entrySet()){
   block(body,label(item.getKey()+"      "+dist(item.getValue()),13,-1),-2,5);
   LinearLayout rail=new LinearLayout(this);rail.setBackground(round(surface,8));
   View fill=new View(this);fill.setBackground(round(accent,8));
   rail.addView(fill,new LinearLayout.LayoutParams(Math.max(px(3),(int)(getResources().getDisplayMetrics().widthPixels*.77*item.getValue()/maxMonth)),px(11)));
   block(body,rail,px(11),14);
  }
 }
 class RouteThumbnail extends View {
  final Paint pen=new Paint(3);final JSONArray coords;
  RouteThumbnail(Context ctx,String serialized){super(ctx);JSONArray parsed;try{parsed=new JSONArray(serialized==null?"[]":serialized);}catch(Exception ex){parsed=new JSONArray();}coords=parsed;setBackground(round(0xff0b1b2b,13));setClipToOutline(true);}
  protected void onDraw(Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();
   pen.setColor(0xff1e3b4e);pen.setStrokeWidth(px(1));pen.setStyle(Paint.Style.STROKE);
   for(int i=1;i<4;i++){canvas.drawLine(w*i/4f,0,w*i/4f,h,pen);canvas.drawLine(0,h*i/4f,w,h*i/4f,pen);}
   if(coords.length()<2)return;
   try{double minLat=90,maxLat=-90,minLon=180,maxLon=-180;
    for(int i=0;i<coords.length();i++){JSONArray p=coords.getJSONArray(i);double lat=p.getDouble(0),lon=p.getDouble(1);minLat=Math.min(minLat,lat);maxLat=Math.max(maxLat,lat);minLon=Math.min(minLon,lon);maxLon=Math.max(maxLon,lon);}
    double lonStretch=Math.max(.00001,(maxLon-minLon)*Math.cos(Math.toRadians((maxLat+minLat)/2)));
    double latSpan=Math.max(.00001,maxLat-minLat);
    Path path=new Path();float firstX=0,firstY=0,lastX=0,lastY=0;
    for(int i=0;i<coords.length();i++){JSONArray p=coords.getJSONArray(i);
     float x=(float)(px(10)+(w-px(20))*((p.getDouble(1)-minLon)*Math.cos(Math.toRadians((maxLat+minLat)/2))/lonStretch));
     float y=(float)(px(10)+(h-px(20))*(1-(p.getDouble(0)-minLat)/latSpan));
     if(i==0){path.moveTo(x,y);firstX=x;firstY=y;}else path.lineTo(x,y);lastX=x;lastY=y;}
    pen.setStrokeWidth(px(3));pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);pen.setColor(accent);canvas.drawPath(path,pen);
    pen.setStyle(Paint.Style.FILL);pen.setColor(0xff61e8cc);canvas.drawCircle(firstX,firstY,px(4),pen);
    pen.setColor(0xfff9a1a9);canvas.drawCircle(lastX,lastY,px(4),pen);pen.setStyle(Paint.Style.STROKE);
   }catch(Exception ignored){}
  }
 }
 void switchRow(LinearLayout parent,String title,String subtitle,String key,boolean initial,Runnable changed){
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(px(15),px(10),px(12),px(10));row.setBackground(round(surface,18));
  LinearLayout details=vertical();TextView headline=label(title,15,0xfff4f8fb);headline.setTypeface(null,1);
  block(details,headline,-2,3);block(details,label(subtitle,12,muted),-2,0);
  row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
  Switch toggle=new Switch(this);toggle.setChecked(getPreferences(0).getBoolean(key,initial));
  toggle.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{accent,0xff9cabb4}));
  row.addView(toggle);
  toggle.setOnCheckedChangeListener((v,checked)->{getPreferences(0).edit().putBoolean(key,checked).apply();changed.run();});
  row.setOnClickListener(v->toggle.setChecked(!toggle.isChecked()));
  block(parent,row,px(69),9);
 }
 void sectionLabel(LinearLayout parent,String text){
  TextView label=label(text,11,accent);label.setTypeface(null,1);
  label.setLetterSpacing(.1f);block(parent,label,-2,9);
 }
 @Override void showSettings(){
  screen=3;init("Settings");headerSmall(body,"Preferences","Keep DriveTrack simple and personal");
  sectionLabel(body,"DRIVING");
  Button units=button("Distance units     ·     "+(miles?"Miles / mph":"Kilometres / km/h"),surface);block(body,units,px(52),10);
  units.setOnClickListener(v->{miles=!miles;getPreferences(0).edit().putBoolean("miles",miles).apply();showSettings();});
  switchRow(body,"Keep screen awake","Keep display visible during active recording","keepAwake",true,()->{keepAwake=getPreferences(0).getBoolean("keepAwake",true);updateScreenAwake();});
  switchRow(body,"Live mini map","Show your location beneath the speedometer","showLiveMap",true,()->{});
  sectionLabel(body,"DISPLAY");
  Button display=button("Minimal speedometer / HUD   ›",surface);block(body,display,px(52),10);
  display.setOnClickListener(v->{getPreferences(0).edit().putBoolean("hud",true).apply();showDashboard();});
  sectionLabel(body,"DATA & BACKUP");
  Button export=button("Export all journeys to JSON",surface);block(body,export,px(52),10);export.setOnClickListener(v->exportJourneys());
  Button restore=button("Restore journeys from .db backup",surface);block(body,restore,px(52),12);restore.setOnClickListener(v->importJourneys());
  block(body,label("GPS readings and journey history are stored on-device. Online maps load OpenStreetMap tiles. Exported files may contain precise locations.",13,muted),-2,18);
  sectionLabel(body,"DATA MANAGEMENT");
  Button cleanup=button("Remove accidental zero-distance entries",surface);block(body,cleanup,px(50),10);
  cleanup.setOnClickListener(v->{
   try(TripStore db=new TripStore(this)){
    int n=db.countEmpty();
    if(n==0){Toast.makeText(this,"No empty journeys found",Toast.LENGTH_SHORT).show();return;}
    new AlertDialog.Builder(this).setTitle("Remove "+n+" empty journeys?")
     .setMessage("Only journeys under 20 metres will be removed. Export a backup first if needed.")
     .setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{
      try(TripStore store=new TripStore(this)){store.deleteEmpty();}
      showSettings();
     }).show();
   }
  });
  sectionLabel(body,"DANGER ZONE");
  Button delete=button("Delete all journeys",0xff713139);block(body,delete,px(48),12);
  delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete all journeys?")
   .setMessage("This permanently deletes stored journeys. Export a backup first.")
   .setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{try(TripStore db=new TripStore(this)){db.deleteAll();}showHistory();}).show());
 }
 void exportTrip(long id,String kind){
  selectedId=id;exportType=kind.equals("GPX")?1:kind.equals("CSV")?2:3;
  Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);save.addCategory(Intent.CATEGORY_OPENABLE);
  save.setType(kind.equals("GPX")?"application/gpx+xml":kind.equals("CSV")?"text/csv":"application/json");
  save.putExtra(Intent.EXTRA_TITLE,"DriveTrack-journey-"+id+"."+kind.toLowerCase(Locale.ROOT));
  startActivityForResult(save,DETAIL_EXPORT);
 }
 String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
 @Override protected void onActivityResult(int req,int result,Intent data){
  if(req!=DETAIL_EXPORT){super.onActivityResult(req,result,data);return;}
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  try(TripStore db=new TripStore(this);Cursor c=db.one(selectedId);
      java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){
   if(out==null||!c.moveToFirst())throw new Exception("Journey not found");
   JSONArray pts=new JSONArray(c.getString(c.getColumnIndexOrThrow("points")));
   StringBuilder s=new StringBuilder();
   if(exportType==1){
    s.append("<?xml version='1.0' encoding='UTF-8'?><gpx version='1.1' creator='DriveTrack' xmlns='http://www.topografix.com/GPX/1/1'><trk><name>DriveTrack journey</name><trkseg>");
    for(int i=0;i<pts.length();i++){JSONArray p=pts.getJSONArray(i);s.append("<trkpt lat='").append(p.getDouble(0)).append("' lon='").append(p.getDouble(1)).append("'/>");}
    s.append("</trkseg></trk></gpx>");
   }else if(exportType==2){
    s.append("point,latitude,longitude\n");
    for(int i=0;i<pts.length();i++){JSONArray p=pts.getJSONArray(i);s.append(i).append(',').append(p.getDouble(0)).append(',').append(p.getDouble(1)).append('\n');}
   }else{
    JSONObject trip=new JSONObject();
    trip.put("started",c.getLong(c.getColumnIndexOrThrow("started")));trip.put("ended",c.getLong(c.getColumnIndexOrThrow("ended")));
    trip.put("distance_metres",c.getDouble(c.getColumnIndexOrThrow("distance")));
    trip.put("max_speed_metres_per_second",c.getDouble(c.getColumnIndexOrThrow("maxspeed")));
    trip.put("points",pts);
    String samples=c.getString(c.getColumnIndexOrThrow("telemetry"));
    trip.put("telemetry",new JSONArray(samples==null?"[]":samples));
    s.append(trip.toString(2));
   }
   out.write(s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
   Toast.makeText(this,"Journey exported",Toast.LENGTH_LONG).show();
  }catch(Exception e){new AlertDialog.Builder(this).setMessage("Export failed: "+e.getMessage()).setPositiveButton("OK",null).show();}
 }
}
