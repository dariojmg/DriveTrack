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
 final int ink=0xff0c131a, surface=0xff192733, accent=0xff16dcb5,muted=0xff93a9b8;
 long selectedId=-1;int exportType=0;int monthOffset=0;String searchTerm="";boolean hud=false;
 static final int DETAIL_EXPORT=880;
 LinearLayout panelRoot; TextView digitalSpeed;boolean keepAwake=true;
 int px(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(px(radius));return g;}
 TextView label(String t,int sp,int col){TextView v=new TextView(this);v.setText(t);v.setTextSize(sp);v.setTextColor(col);return v;}
 LinearLayout.LayoutParams margin(int height,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:height);p.bottomMargin=px(bottom);return p;}
 LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 void block(LinearLayout parent,View v,int height,int gap){parent.addView(v,margin(height,gap));}
 void headerSmall(LinearLayout parent,String heading,String sub){
  TextView title=label(heading,20,0xffffffff);title.setTypeface(null,1);block(parent,title,-2,2);
  if(sub!=null){TextView desc=label(sub,12,muted);block(parent,desc,-2,10);}
 }
 Button button(String text,int color){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(-1);b.setBackgroundTintList(ColorStateList.valueOf(color));return b;}
 LinearLayout tile(String title,String value){
  LinearLayout l=vertical();l.setPadding(px(13),px(10),px(12),px(10));l.setBackground(round(surface,14));
  block(l,label(title,11,muted),-2,6);
  TextView v=label(value,22,-1);v.setTypeface(null,1);l.addView(v);return l;
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
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(px(16),px(7),px(16),px(8));
  TextView brand=label("DT  DriveTrack",19,accent);brand.setTypeface(null,1);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
  if(title.equals("Dashboard")){
   TextView gps=label(TrackingService.active?(TrackingService.paused?"Ⅱ PAUSED":TrackingService.lastFixTime>0?"● RECORDING":"○ ACQUIRING"):"○ WAITING FOR GPS",11,accent);header.addView(gps);
  }
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
  root.addView(nav);
 }
 LinearLayout roundJourneyControl(int iconRes,String title,int color){
  GradientDrawable circle=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
   new int[]{lightenButton(color),color,darkenButton(color)});
  circle.setShape(GradientDrawable.OVAL);
  circle.setStroke(px(2),0x88ffffff);
  android.graphics.drawable.RippleDrawable ripple=new android.graphics.drawable.RippleDrawable(
    ColorStateList.valueOf(0x66ffffff),circle,null);
  LinearLayout control=vertical();
  control.setGravity(Gravity.CENTER);
  control.setBackground(ripple);
  control.setElevation(px(5));
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
  LinearLayout signal=new LinearLayout(this);signal.setPadding(px(12),px(9),px(12),px(9));signal.setGravity(Gravity.CENTER_VERTICAL);signal.setBackground(round(surface,12));
  signal.addView(label("GPS SIGNAL",11,muted),new LinearLayout.LayoutParams(0,-2,1));
  gpsValue=label("Waiting for fix",13,-1);signal.addView(gpsValue);
  block(body,signal,px(43),7);
  FrameLayout dial=new FrameLayout(this);
  gauge=new Gauge(this);dial.addView(gauge,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
  // Center round console-style controls beneath the dial, consistently in every journey state.
  FrameLayout.LayoutParams frame=new FrameLayout.LayoutParams(-1,px(100),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
  frame.bottomMargin=px(31);dial.addView(actions,frame);
  if(!TrackingService.active){
   LinearLayout start=roundJourneyControl(R.drawable.ic_drive_start,"START",0xff078c62);
   actions.addView(start,new LinearLayout.LayoutParams(px(94),px(94)));
   start.setOnClickListener(v->{if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},42);
    else startJourney();});
  }else{
   LinearLayout pause=roundJourneyControl(TrackingService.paused?R.drawable.ic_drive_resume:R.drawable.ic_drive_pause,TrackingService.paused?"RESUME":"PAUSE",0xffd58a12);
   LinearLayout stop=roundJourneyControl(R.drawable.ic_drive_stop,"STOP",0xffd63e47);
   LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(px(94),px(94));left.rightMargin=px(14);
   actions.addView(pause,left);actions.addView(stop,new LinearLayout.LayoutParams(px(94),px(94)));
   pause.setOnClickListener(v->{startService(new Intent(this,TrackingService.class).setAction(TrackingService.paused?"RESUME":"PAUSE"));handler.postDelayed(()->showDashboard(),200);});
   stop.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Save this journey?")
    .setMessage("The trip will be saved to History and the dashboard will reset.")
    .setNegativeButton("Cancel",null).setPositiveButton("Save & stop",(d,w)->{startService(new Intent(this,TrackingService.class).setAction("STOP"));handler.postDelayed(()->showDashboard(),300);}).show());
  }
  LinearLayout.LayoutParams dialLayout=new LinearLayout.LayoutParams(-1,0,1);body.addView(dial,dialLayout);
  dashboardStats=new LinearLayout(this);
  distanceValue=compactCard(dashboardStats,"DISTANCE","0.00 km",true);
  driveTimeValue=compactCard(dashboardStats,"DURATION","00:00",false);
  block(body,dashboardStats,px(83),8);
  LinearLayout place=tile("CURRENT LOCATION","Waiting for GPS");
  currentLocationValue=(TextView)place.getChildAt(1);currentLocationValue.setTextSize(14);currentLocationValue.setSingleLine(true);
  currentLocationValue.setEllipsize(android.text.TextUtils.TruncateAt.END);
  block(body,place,px(71),8);
  updateDashboard();
 }
 @Override void updateDashboard(){
  updateScreenAwake();
  if(screen==0){
   if(gpsValue!=null)gpsValue.setText(!TrackingService.active?"○ Waiting for GPS":TrackingService.paused?"Ⅱ Paused":TrackingService.lastFixTime>0&&System.currentTimeMillis()-TrackingService.lastFixTime>8000?"○ Signal lost":TrackingService.accuracy<=10&&TrackingService.lastFixTime>0?"● Strong · ±"+(int)TrackingService.accuracy+" m":TrackingService.accuracy<=25&&TrackingService.lastFixTime>0?"● Fair · ±"+(int)TrackingService.accuracy+" m":TrackingService.lastFixTime>0?"○ Poor · ±"+(int)TrackingService.accuracy+" m":"○ Acquiring GPS");
   if(distanceValue!=null)distanceValue.setText(dist(TrackingService.distance));
   if(driveTimeValue!=null)driveTimeValue.setText(TrackingService.active?duration(TrackingService.elapsedActiveMs()):"00:00");
   if(currentLocationValue!=null)currentLocationValue.setText(TrackingService.active?TrackingService.locationText:"Waiting for GPS");
   if(gauge!=null)gauge.invalidate();
  }else if(screen==4&&digitalSpeed!=null)digitalSpeed.setText(String.valueOf(Math.round(TrackingService.currentSpeed*(miles?2.236936:3.6))));
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
   int count=0;double sum=0;while(c.moveToNext()){count++;sum+=c.getDouble(c.getColumnIndexOrThrow("distance"));}
   twoStats(body,"TOTAL DISTANCE",dist(sum),"JOURNEYS",String.valueOf(count));
  }
  Button insights=button("▥  Driving insights",surface);block(body,insights,px(48),8);insights.setOnClickListener(v->showInsights());
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
     LinearLayout card=vertical();card.setPadding(px(14),px(12),px(14),px(12));card.setBackground(round(surface,15));
     LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
     TextView tripName=label(title,17,-1);tripName.setTypeface(null,1);
     heading.addView(tripName,new LinearLayout.LayoutParams(0,-2,1));
     TextView menu=label("⋮",25,muted);menu.setGravity(Gravity.CENTER);
     menu.setContentDescription("Options for "+title);menu.setClickable(true);
     heading.addView(menu,new LinearLayout.LayoutParams(px(44),px(42)));
     block(card,heading,-2,2);
     block(card,label(date(start)+"    ·    "+duration(time),12,muted),-2,7);
     block(card,label(dis+"     •     Max "+speed(max),14,accent),-2,0);
     block(entries,card,-2,8);
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
  Button export=button("Export all journeys · JSON",surface);block(body,export,px(48),16);export.setOnClickListener(v->exportJourneys());
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
   twoStats(body,"OVERALL AVG",speed(metres/Math.max(1,effective/1000d)),"STOPPED",duration(Math.max(0,effective-moving)));
   twoStats(body,"PAUSED",duration(pause),"GPS POINTS",String.valueOf(coords.length()));
   String telemetry=c.getString(c.getColumnIndexOrThrow("telemetry"));
   try{JSONArray samples=new JSONArray(telemetry==null?"[]":telemetry);
    block(body,label(samples.length()==0?"GPS diagnostics unavailable for this older journey":"GPS diagnostics: "+samples.length()+" speed samples",13,muted),-2,9);
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
 @Override void showSettings(){
  screen=3;init("Settings");headerSmall(body,"Preferences","Keep DriveTrack simple and personal");
  Button units=button("Units: "+(miles?"Miles · mph":"Kilometres · km/h"),surface);block(body,units,px(52),10);
  units.setOnClickListener(v->{miles=!miles;getPreferences(0).edit().putBoolean("miles",miles).apply();showSettings();});
  Button awake=button("Keep screen awake while recording: "+(getPreferences(0).getBoolean("keepAwake",true)?"ON":"OFF"),surface);
  block(body,awake,px(52),10);
  awake.setOnClickListener(v->{boolean value=!getPreferences(0).getBoolean("keepAwake",true);getPreferences(0).edit().putBoolean("keepAwake",value).apply();keepAwake=value;updateScreenAwake();showSettings();});
  Button display=button("Open minimal HUD speedometer",surface);block(body,display,px(52),10);
  display.setOnClickListener(v->{getPreferences(0).edit().putBoolean("hud",true).apply();showDashboard();});
  Button export=button("Export all journeys to JSON",surface);block(body,export,px(52),10);export.setOnClickListener(v->exportJourneys());
  Button restore=button("Restore journeys from .db backup",surface);block(body,restore,px(52),12);restore.setOnClickListener(v->importJourneys());
  block(body,label("GPS readings and journey history are stored on-device. Online maps load OpenStreetMap tiles. Exported files may contain precise locations.",13,muted),-2,18);
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
